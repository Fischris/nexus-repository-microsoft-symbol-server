package org.sonatype.nexus.plugins.microsoftsymbolserver.internal.hosted;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.microsoftsymbolserver.internal.MicrosoftSymbolServerFormat;
import org.sonatype.nexus.plugins.microsoftsymbolserver.internal.MicrosoftSymbolServerRecipeSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.http.HttpHandlers;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.view.*;
import org.sonatype.nexus.repository.view.matchers.ActionMatcher;
import org.sonatype.nexus.repository.view.matchers.SuffixMatcher;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import static org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers.and;


@Named(MircroSoftSymbolServerHostedRecipe.NAME)
@Singleton
public class MircroSoftSymbolServerHostedRecipe extends MicrosoftSymbolServerRecipeSupport
{

    public static final String NAME = "microsoftsymbolserver-hosted";
    @Inject
    MircroSoftSymbolServerHostedRecipe(@Named(HostedType.NAME) final Type type,
                                       @Named(MicrosoftSymbolServerFormat.NAME) final Format format)
    {
        super(type, format);
    }

    @Override
    public void apply(@Nonnull final Repository repository) throws Exception {
        repository.attach(getSecurityFacet().get());
        repository.attach(configure(getViewFacet().get()));
        repository.attach(getMssContentFacet().get());
        repository.attach(getStorageFacet().get());
        repository.attach(getAttributesFacet().get());
        repository.attach(getComponentMaintenanceFacet().get());
        repository.attach(getSearchFacet().get());
    }

    /**
     * Configure {@link ViewFacet}.
     */
    private ViewFacet configure(final ConfigurableViewFacet facet) {
        Router.Builder builder = new Router.Builder();

        // Additional handlers, such as the lastDownloadHandler, are intentionally
        // not included on this route because this route forwards to the route below.
        // This route specifically handles GET / and forwards to /index.html.
        builder.route(new Route.Builder()
                .matcher(and(new ActionMatcher(HttpMethods.GET), new SuffixMatcher("/")))
                .handler(getTimingHandler())
                .handler(getIndexHtmlForwardHandler())
                .create());


        builder.route(new Route.Builder()
                .matcher(new TokenMatcher("/{name:.+}"))
                .handler(getTimingHandler())
                .handler(getSecurityHandler())
                .handler(getExceptionHandler())
                .handler(getHandlerContributor())
                .handler(getConditionalRequestHandler())
                .handler(getPartialFetchHandler())
                .handler(getContentHeadersHandler())
                .handler(getUnitOfWorkHandler())
                .handler(getLastDownloadedHandler())
                .handler(getMssContentHandler())
                .create());

        builder.defaultHandlers(HttpHandlers.badRequest());

        facet.configure(builder.create());

        return facet;
    }

}