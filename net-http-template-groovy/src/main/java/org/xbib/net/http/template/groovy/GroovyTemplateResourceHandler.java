package org.xbib.net.http.template.groovy;

import org.xbib.net.Resource;
import org.xbib.net.http.server.HttpServerContext;
import org.xbib.net.http.server.resource.HtmlTemplateResourceHandler;

import java.io.IOException;
import java.nio.file.Path;

public class GroovyTemplateResourceHandler extends HtmlTemplateResourceHandler {

    public GroovyTemplateResourceHandler() {
        this(null, "gtpl", "index.gtpl");
    }

    public GroovyTemplateResourceHandler(Path prefix, String suffix, String indexFileName) {
        super(prefix, suffix, indexFileName);
    }

    @Override
    protected Resource createResource(HttpServerContext httpServerContext) throws IOException {
        return new GroovyTemplateResource(this, httpServerContext);
    }
}
