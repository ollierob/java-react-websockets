package net.ollie.jrw;

import com.google.inject.AbstractModule;
import net.ollie.jrw.resource.ResourceModule;

public class ServerModule extends AbstractModule {

    @Override
    protected void configure() {
        super.configure();
        this.install(new ResourceModule());
    }

}
