package net.ollie.jrw.resource;

import com.google.inject.AbstractModule;

public class ResourceModule extends AbstractModule {

    @Override
    protected void configure() {
        super.configure();
        this.bind(JavascriptResource.class);
        this.bind(ChatResource.class);
    }

}
