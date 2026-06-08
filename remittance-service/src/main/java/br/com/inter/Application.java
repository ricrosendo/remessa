package br.com.inter;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.Micronaut;

public class Application {

    public static void main(String[] args) {
        start(args);
    }

    static ApplicationContext start(String[] args) {
        return Micronaut.run(Application.class, args);
    }
}