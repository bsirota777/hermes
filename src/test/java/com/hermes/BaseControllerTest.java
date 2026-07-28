package com.hermes;

import com.hermes.exception.GlobalExceptionHandler;
import com.hermes.security.JwtService;
import com.hermes.user.UserService;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(GlobalExceptionHandler.class)
public abstract class BaseControllerTest {

    @MockitoBean
    protected JwtService jwtService;

    @MockitoBean
    protected UserService userService;
}