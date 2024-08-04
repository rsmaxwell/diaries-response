package com.rsmaxwell.diaries.response.utilities;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Controller;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Controller
public @interface CustomController {
	// Define any additional attributes if needed
}
