/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2019-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.plugins.microsoftsymbolserver.internal;

import org.apache.http.client.entity.EntityBuilder;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.testsuite.testsupport.FormatClientSupport;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.is;



public class MicrosoftSymbolServerHostedIT
    extends MicrosoftSymbolServerITSupport
{
  private static final String TEST_PATH = "imaginary/path/System.Core.pdb";
  private static final String TEST_PDB_NAME = "thisIsA.pdb";
  private static final String TEST_PDB_01 = "thisIsA.pdb";
  private static final String TEST_GUID_AGE_PDB_01 = "1ED7865A6A674D138B99D1D423505AD21";
  private static final String TEST_PDB_02 = "thisIsAnother.pdb";
  private static final String TEST_GUID_AGE_PDB_02 = "CC4BF188B8B54686B042D8873D4C89D61";

  private MicrosoftSymbolServerHostedClient hostedClient;
  private Repository hostedRepo;

  @Configuration
  public static Option[] configureNexus() {
    return NexusPaxExamSupport.options(
        NexusITSupport.configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-microsoft-symbol-server")
    );
  }


  @Test
  public void unresponsiveRemoteProduces404() throws Exception {

    hostedRepo = repos.createMicrosoftSymbolServerHosted("microsoft-symbol-server-test-hosted-notfound");
    hostedClient = microsoftSymbolServerHostedClient(hostedRepo);

    MatcherAssert.assertThat(FormatClientSupport.status(hostedClient.get(TEST_PATH)), is(HttpStatus.NOT_FOUND));
  }


  @Test
  public void upAndDownloadPdbToServer() throws Exception {

    hostedRepo = repos.createMicrosoftSymbolServerHosted("microsoft-symbol-server-test-hosted");
    hostedClient = microsoftSymbolServerHostedClient(hostedRepo);

    final String TEST_GROUP = "testgroup";
    final String TEST_COMPONENT = "testcomp";
    final String TEST_VERSION = "testver";

    assertThat(hostedClient.put(TEST_PDB_NAME +
                    "?g="+ TEST_GROUP+
                    "&c="+ TEST_COMPONENT,
            EntityBuilder.create().setStream(getClass().getResource(TEST_PDB_01).openStream()).build())
                    .getStatusLine().getStatusCode(),
            is(HttpStatus.BAD_REQUEST));


    assertThat(hostedClient.put(TEST_PDB_NAME +
                    "?g="+ TEST_GROUP+
                    "&c="+ TEST_COMPONENT +
                    "&v="+ TEST_VERSION,
            EntityBuilder.create().setStream(getClass().getResource(TEST_PDB_01).openStream()).build())
                    .getStatusLine().getStatusCode(),
            is(HttpStatus.CREATED));


    Asset asset = findAsset(hostedRepo, TEST_PDB_NAME+"/"+ TEST_GUID_AGE_PDB_01+"/"+TEST_PDB_NAME);
    Component component = findComponent(hostedRepo, TEST_COMPONENT);

    assertThat(asset.componentId(), is(component.getEntityMetadata().getId()));
    assertThat(component.name(), is(TEST_COMPONENT));
    assertThat(component.version(), is(TEST_VERSION));
    assertThat(component.group(), is(TEST_GROUP));

    assertThat(hostedClient.get(TEST_PDB_NAME+"/"+ TEST_GUID_AGE_PDB_01+"/"+TEST_PDB_NAME).getStatusLine().getStatusCode(),
            is(HttpStatus.OK));


    //uploading other pdb with same name/group/version/compname should cause another guid+age:
    hostedClient.put(TEST_PDB_NAME +
                    "?g="+ TEST_GROUP+
                    "&c="+ TEST_COMPONENT +
                    "&v="+ TEST_VERSION,
            EntityBuilder.create().setStream(getClass().getResource(TEST_PDB_02).openStream()).build());

    assertThat(hostedClient.get(TEST_PDB_NAME+"/"+ TEST_GUID_AGE_PDB_01+"/"+TEST_PDB_NAME).getStatusLine().getStatusCode(),
            is(HttpStatus.NOT_FOUND));

    assertThat(hostedClient.get(TEST_PDB_NAME+"/"+ TEST_GUID_AGE_PDB_02+"/"+TEST_PDB_NAME).getStatusLine().getStatusCode(),
            is(HttpStatus.OK));
  }
}
