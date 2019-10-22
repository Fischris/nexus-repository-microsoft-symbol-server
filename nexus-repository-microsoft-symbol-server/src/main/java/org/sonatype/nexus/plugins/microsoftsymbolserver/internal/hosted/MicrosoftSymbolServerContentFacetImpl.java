/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.*/


package org.sonatype.nexus.plugins.microsoftsymbolserver.internal.hosted;

import java.io.*;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.plugins.microsoftsymbolserver.internal.hosted.pdb_file.ParseException;
import org.sonatype.nexus.plugins.microsoftsymbolserver.internal.hosted.pdb_file.Pdbparser;
import org.sonatype.nexus.plugins.microsoftsymbolserver.internal.util.MicrosoftSymbolServerDataAccess;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.storage.*;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Parameters;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.transaction.UnitOfWork;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;


@Named
public class MicrosoftSymbolServerContentFacetImpl
        extends FacetSupport implements MicrosoftSymbolServerContentFacet

{
    private static final List<HashAlgorithm> hashAlgorithms = Arrays.asList(MD5, SHA1);

    private final AssetEntityAdapter assetEntityAdapter;

    @Inject
    public MicrosoftSymbolServerContentFacetImpl(final AssetEntityAdapter assetEntityAdapter) {
        this.assetEntityAdapter = checkNotNull(assetEntityAdapter);
    }


    //do not remove
    @Override
    protected void doValidate(final Configuration configuration) throws Exception {
        // empty
    }

    @Nullable
    @TransactionalTouchBlob
    public Content get(final String path) {
        StorageTx tx = UnitOfWork.currentTx();

        final Asset asset = findAsset(tx, path);
        if (asset == null) {
            return null;
        }

        final Blob blob = tx.requireBlob(asset.requireBlobRef());
        return toContent(asset, blob);
    }


    @TransactionalStoreBlob
    public Content put(String path, final Payload content, Parameters parameters) throws IOException, ParseException {

        checkIfFilenameIsPdb(path);

        StorageFacet storageFacet = facet(StorageFacet.class);
        try (TempBlob tempBlob = storageFacet.createTempBlob(content, hashAlgorithms)) {
            return doPutContent(path, tempBlob, content, parameters);
        }

    }

    @TransactionalStoreBlob
    protected Content doPutContent(String path, final TempBlob tempBlob, final Payload payload, Parameters parameters)
            throws IOException, ParseException
    {

        // manipulating path
        // pdb / guid / pdb
        //  --> symbol-file-download-path


        String assetNameWithoutGuid = path;

        path = createPathWithGuidandAge(path, tempBlob);

        StorageTx tx = UnitOfWork.currentTx();

        Asset asset = CreateAssetAndDeleteIfExists(path, assetNameWithoutGuid,
                parameters.get("c"), parameters.get("g"), parameters.get("v"));

        AttributesMap contentAttributes = null;
        if (payload instanceof Content) {
            contentAttributes = ((Content) payload).getAttributes();
        }


        Content.applyToAsset(asset, Content.maintainLastModified(asset, contentAttributes));
        AssetBlob assetBlob = tx.setBlob(
                asset,
                path,
                tempBlob,
                null,
                payload.getContentType(),
                false
        );


        tx.saveAsset(asset);
        return toContent(asset, assetBlob.getBlob());
    }


    private String createPathWithGuidandAge(String path, TempBlob tempBlob) throws ParseException{
        return path + "/" + new Pdbparser().getGuidWithAge(tempBlob) + "/" + path;
    }

    private void checkIfFilenameIsPdb(String path) throws ParseException{

        if (!path.endsWith(".pdb")){
            throw new ParseException("File should end with .pdb");
        }
    }


    @TransactionalStoreMetadata
    public Asset CreateAssetAndDeleteIfExists(final String assetName, final String assetNameWithoutGuid,
                                              final String componentName, final String componentGroup, final String version) {
        final StorageTx tx = UnitOfWork.currentTx();

        final Bucket bucket = tx.findBucket(getRepository());

        Component component = MicrosoftSymbolServerDataAccess.findComponent(tx, getRepository(), componentGroup, componentName, version);

        Asset asset;
        if (component == null) {

            // CREATE if component doesn't exist
            asset = createComponent(tx, bucket, componentGroup, componentName,  version, assetName);

        } else {

            // search for assets with same file name, version, group and componentName.
            // if true, DELETE old first

            Iterable<Asset> assets = tx.browseAssets(component);
            for (Asset ass : assets){
                if (ass.name().endsWith("/"+assetNameWithoutGuid)){
                    //asset = ass;
                    tx.deleteAsset(ass);
                    break;
                }
            }

            asset =  tx.createAsset(bucket, component).name(assetName);
        }

        return asset;
    }


    private Asset createComponent(StorageTx tx, Bucket bucket, String componentGroup, String componentName, String version, String assetName){

    Component component = tx.createComponent(bucket, getRepository().getFormat())
            .group(componentGroup)
            .name(componentName)
            .version(version)
    ;

    tx.saveComponent(component);

    Asset asset = tx.createAsset(bucket, component);
    asset.name(assetName);

    return asset;
    }


    private Asset findAsset(StorageTx tx, String path) {
        return tx.findAssetWithProperty(P_NAME, path, tx.findBucket(getRepository()));
    }

    private Content toContent(final Asset asset, final Blob blob) {
        final Content content = new Content(new BlobPayload(blob, asset.requireContentType()));
        Content.extractFromAsset(asset, hashAlgorithms, content.getAttributes());
        return content;
    }



}