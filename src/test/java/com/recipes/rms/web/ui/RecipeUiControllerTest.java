package com.recipes.rms.web.ui;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Renders every page through MockMvc so template errors fail fast, without a browser. */
@SpringBootTest
@AutoConfigureMockMvc
class RecipeUiControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/recipes").accept(MediaType.TEXT_HTML)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("login"));
    }

    @Test
    @WithMockUser(username = "author", roles = "AUTHOR")
    void listWithFiltersRenders() throws Exception {
        mvc.perform(get("/recipes").param("cuisine", "Italian").param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Margherita Pizza")))
                .andExpect(content().string(not(containsString("Tiramisu"))));
    }

    @Test
    @WithMockUser(username = "author", roles = "AUTHOR")
    void detailShowsSubmitButtonForOwnDraft() throws Exception {
        mvc.perform(get("/recipes/{id}", findIdByTitle("Chana Masala")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action-SUBMIT")))
                .andExpect(content().string(containsString("edit-button")));
    }

    @Test
    @WithMockUser(username = "reviewer", roles = "REVIEWER")
    void detailShowsReviewButtonsAndHistoryForReviewer() throws Exception {
        mvc.perform(get("/recipes/{id}", findIdByTitle("Tiramisu")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action-APPROVE")))
                .andExpect(content().string(containsString("action-REJECT")))
                .andExpect(content().string(containsString("history-table")))
                .andExpect(content().string(not(containsString("edit-button"))));
    }

    @Test
    @WithMockUser(username = "author", roles = "AUTHOR")
    void formsRender() throws Exception {
        mvc.perform(get("/recipes/new")).andExpect(status().isOk()).andExpect(view().name("recipes/form"));
        mvc.perform(get("/recipes/{id}/edit", findIdByTitle("Chana Masala"))).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "author", roles = "AUTHOR")
    void invalidFormIsRedisplayedWithErrors() throws Exception {
        mvc.perform(post("/recipes").with(csrf()).param("title", "ab"))
                .andExpect(status().isOk())
                .andExpect(view().name("recipes/form"))
                .andExpect(model().attributeHasFieldErrors("recipeForm", "title", "cuisine", "ingredients"));
    }

    @Test
    @WithMockUser(username = "reviewer", roles = "REVIEWER")
    void reviewerCannotOpenCreateForm() throws Exception {
        mvc.perform(get("/recipes/new")).andExpect(status().isForbidden()).andExpect(view().name("error"));
    }

    @Test
    @WithMockUser(username = "reviewer", roles = "REVIEWER")
    void invalidTransitionShowsFlashErrorInsteadOfFailing() throws Exception {
        long id = findIdByTitle("Margherita Pizza");
        mvc.perform(post("/recipes/{id}/transition", id).with(csrf()).param("action", "APPROVE"))
                .andExpect(redirectedUrl("/recipes/" + id));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN", "AUTHOR", "REVIEWER"})
    void dashboardRenders() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("total-recipes")))
                .andExpect(content().string(containsString("count-PUBLISHED")))
                .andExpect(content().string(containsString("Main Course")));
    }

    @Test
    @WithMockUser(username = "author", roles = "AUTHOR")
    void unknownRecipeRenders404Page() throws Exception {
        mvc.perform(get("/recipes/{id}", 987654)).andExpect(status().isNotFound()).andExpect(view().name("error"));
    }

    private long findIdByTitle(String title) throws Exception {
        String html = mvc.perform(get("/recipes").param("title", title))
                .andReturn().getResponse().getContentAsString();
        int marker = html.indexOf("/recipes/", html.indexOf("recipe-row"));
        int end = html.indexOf('"', marker);
        return Long.parseLong(html.substring(marker + "/recipes/".length(), end));
    }
}
