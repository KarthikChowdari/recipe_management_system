package com.recipes.rms.web.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecipeApiControllerTest {

    static final String RECIPE_JSON = """
            {"title":"%s","cuisine":"Thai","category":"Main Course",
             "ingredients":"noodles\\ntofu","instructions":"Stir fry.","servings":2}
            """;

    @Autowired
    MockMvc mvc;

    long createRecipe(String title, String user, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/recipes").with(httpBasic(user, password))
                        .contentType(MediaType.APPLICATION_JSON).content(RECIPE_JSON.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    @Test
    void unauthenticatedApiCallGets401NotRedirect() throws Exception {
        mvc.perform(get("/api/recipes")).andExpect(status().isUnauthorized());
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void authorCreatesAndReadsRecipe() throws Exception {
        mvc.perform(post("/api/recipes").with(httpBasic("author", "author123"))
                        .contentType(MediaType.APPLICATION_JSON).content(RECIPE_JSON.formatted("Pad Thai")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/recipes/")))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdBy").value("author"));

        long id = createRecipe("Green Curry", "author", "author123");
        mvc.perform(get("/api/recipes/{id}", id).with(httpBasic("reviewer", "reviewer123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Green Curry"));
    }

    @Test
    void invalidPayloadReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/recipes").with(httpBasic("author", "author123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.cuisine").exists());
    }

    @Test
    void reviewerCannotCreateRecipe() throws Exception {
        mvc.perform(post("/api/recipes").with(httpBasic("reviewer", "reviewer123"))
                        .contentType(MediaType.APPLICATION_JSON).content(RECIPE_JSON.formatted("Nope")))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerUpdatesDraft() throws Exception {
        long id = createRecipe("Tom Yum", "author", "author123");

        mvc.perform(put("/api/recipes/{id}", id).with(httpBasic("author", "author123"))
                        .contentType(MediaType.APPLICATION_JSON).content(RECIPE_JSON.formatted("Tom Yum Goong")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Tom Yum Goong"));
    }

    @Test
    void unknownRecipeReturns404() throws Exception {
        mvc.perform(get("/api/recipes/{id}", 999999).with(httpBasic("author", "author123")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Recipe 999999 not found"));
    }

    @Test
    void onlyAdminDeletes() throws Exception {
        long id = createRecipe("Massaman", "author", "author123");

        mvc.perform(delete("/api/recipes/{id}", id).with(httpBasic("author", "author123")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/recipes/{id}", id).with(httpBasic("admin", "admin123")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/recipes/{id}", id).with(httpBasic("admin", "admin123")))
                .andExpect(status().isNotFound());
    }

    void transition(long id, String action, String user, String password, int expectedStatus) throws Exception {
        mvc.perform(post("/api/recipes/{id}/transitions", id).with(httpBasic(user, password))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"" + action + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void searchFiltersByCuisineStatusAndTitle() throws Exception {
        mvc.perform(get("/api/recipes").param("cuisine", "italian").param("status", "PUBLISHED")
                        .with(httpBasic("author", "author123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].cuisine", everyItem(equalTo("Italian"))))
                .andExpect(jsonPath("$.content[*].status", everyItem(equalTo("PUBLISHED"))))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)));

        mvc.perform(get("/api/recipes").param("title", "carbon").with(httpBasic("author", "author123")))
                .andExpect(jsonPath("$.content[0].title").value("Spaghetti Carbonara"));
    }

    @Test
    void invalidStatusFilterIs400() throws Exception {
        mvc.perform(get("/api/recipes").param("status", "BOGUS").with(httpBasic("author", "author123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fullReviewWorkflowWithHistory() throws Exception {
        long id = createRecipe("Khao Soi", "author", "author123");

        transition(id, "APPROVE", "reviewer", "reviewer123", 409);   // cannot approve a draft
        transition(id, "SUBMIT", "reviewer", "reviewer123", 403);    // reviewer is not the owner/author
        transition(id, "SUBMIT", "author", "author123", 200);
        transition(id, "APPROVE", "author", "author123", 403);       // authors cannot approve
        transition(id, "REJECT", "reviewer", "reviewer123", 200);
        transition(id, "SUBMIT", "author", "author123", 200);
        transition(id, "APPROVE", "reviewer", "reviewer123", 200);

        mvc.perform(get("/api/recipes/{id}", id).with(httpBasic("author", "author123")))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(get("/api/recipes/{id}/history", id).with(httpBasic("author", "author123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].action").value("APPROVE"))
                .andExpect(jsonPath("$[0].changedBy").value("reviewer"));

        // published recipes are locked for their author
        mvc.perform(put("/api/recipes/{id}", id).with(httpBasic("author", "author123"))
                        .contentType(MediaType.APPLICATION_JSON).content(RECIPE_JSON.formatted("Too late")))
                .andExpect(status().isForbidden());
    }

    @Test
    void transitionWithoutActionIs400() throws Exception {
        long id = createRecipe("Larb", "author", "author123");
        mvc.perform(post("/api/recipes/{id}/transitions", id).with(httpBasic("author", "author123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.action").exists());
    }

    @Test
    void dashboardReturnsCountsForEveryStatus() throws Exception {
        mvc.perform(get("/api/dashboard").with(httpBasic("reviewer", "reviewer123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecipes", greaterThanOrEqualTo(12)))
                .andExpect(jsonPath("$.countByStatus.DRAFT").exists())
                .andExpect(jsonPath("$.countByStatus.PENDING_REVIEW").exists())
                .andExpect(jsonPath("$.countByStatus.PUBLISHED").exists())
                .andExpect(jsonPath("$.topCategories[0].name").value("Main Course"))
                .andExpect(jsonPath("$.topCuisines.length()").value(5));
    }
}
