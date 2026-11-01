package com.recipes.rms.web.ui;

import com.recipes.rms.service.ConflictException;
import com.recipes.rms.service.OperationNotPermittedException;
import com.recipes.rms.service.RecipeNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** Renders the shared error page for exceptions raised by the browser UI controllers. */
@ControllerAdvice(basePackages = "com.recipes.rms.web.ui")
public class UiExceptionHandler {

    @ExceptionHandler(RecipeNotFoundException.class)
    String notFound(RecipeNotFoundException ex, Model model, HttpServletResponse response) {
        return render(HttpStatus.NOT_FOUND, ex.getMessage(), model, response);
    }

    @ExceptionHandler(OperationNotPermittedException.class)
    String forbidden(OperationNotPermittedException ex, Model model, HttpServletResponse response) {
        return render(HttpStatus.FORBIDDEN, ex.getMessage(), model, response);
    }

    @ExceptionHandler(ConflictException.class)
    String conflict(ConflictException ex, Model model, HttpServletResponse response) {
        return render(HttpStatus.CONFLICT, ex.getMessage(), model, response);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    String optimisticLock(Model model, HttpServletResponse response) {
        return render(HttpStatus.CONFLICT, "The recipe was modified concurrently. Reload and try again.",
                model, response);
    }

    private String render(HttpStatus status, String message, Model model, HttpServletResponse response) {
        response.setStatus(status.value());
        model.addAttribute("status", status.value());
        model.addAttribute("error", status.getReasonPhrase());
        model.addAttribute("message", message);
        return "error";
    }
}
