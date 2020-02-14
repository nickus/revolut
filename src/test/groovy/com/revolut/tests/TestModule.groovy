package com.revolut.tests

import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.revolut.Application
import com.revolut.infrastructure.Database
import org.junit.BeforeClass
import spock.lang.Shared

class TestModule extends AbstractModule {
    @Shared
    static Injector mainContainer

    @BeforeClass
    public static void beforeClass() throws Exception {
    }

    @Override
    protected void configure() {
        if (mainContainer == null) mainContainer = Application.run()
        def database = mainContainer.getInstance(Database)
        def fixtures = new Fixtures(database)
        bind(Fixtures).toInstance(fixtures)
    }
}
