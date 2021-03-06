/*
 * Copyright (c) 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.moe.client.dvcs;

import static org.easymock.EasyMock.expect;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.LocalWorkspace;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import java.io.File;
import java.util.Collections;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

public class AbstractDvcsCodebaseCreatorTest extends TestCase {
  private static final String MOCK_REPO_NAME = "mockrepo";

  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem mockFS = control.createMock(FileSystem.class);
  private final RepositoryConfig mockRepoConfig = control.createMock(RepositoryConfig.class);

  private final LocalWorkspace mockRepo = control.createMock(LocalWorkspace.class);
  private final RevisionHistory mockRevHistory = control.createMock(RevisionHistory.class);
  private final AbstractDvcsCodebaseCreator codebaseCreator =
      new AbstractDvcsCodebaseCreator(
          null, mockFS, Suppliers.ofInstance(mockRepo), mockRevHistory, "public") {
        @Override
        protected LocalWorkspace cloneAtLocalRoot(String localroot) {
          throw new UnsupportedOperationException();
        }
      };

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    expect(mockRepo.getConfig()).andReturn(mockRepoConfig).anyTimes();
    expect(mockRepoConfig.getIgnoreFilePatterns()).andReturn(ImmutableList.<String>of());
    expect(mockRepo.getRepositoryName()).andReturn(MOCK_REPO_NAME);
  }

  public void testCreate_noGivenRev() throws Exception {
    String archiveTempDir = "/tmp/git_archive_mockrepo_head";
    // Short-circuit Utils.filterFilesByPredicate(ignore_files_re).
    expect(mockFS.findFiles(new File(archiveTempDir))).andReturn(ImmutableSet.<File>of());

    expect(mockRevHistory.findHighestRevision(null))
        .andReturn(Revision.create("mock head changeset ID", MOCK_REPO_NAME));
    expect(mockRepo.archiveAtRevision("mock head changeset ID"))
        .andReturn(new File(archiveTempDir));

    control.replay();

    Codebase codebase = codebaseCreator.create(Collections.<String, String>emptyMap());

    assertEquals(new File(archiveTempDir), codebase.path());
    assertEquals("public", codebase.projectSpace());
    assertEquals("mockrepo", codebase.expression().toString());

    control.verify();
  }

  public void testCreate_givenRev() throws Exception {
    String givenRev = "givenrev";
    String archiveTempDir = "/tmp/git_reclone_mockrepo_head_" + givenRev;
    // Short-circuit Utils.filterFilesByPredicate(ignore_files_re).
    expect(mockFS.findFiles(new File(archiveTempDir))).andReturn(ImmutableSet.<File>of());

    expect(mockRevHistory.findHighestRevision(givenRev))
        .andReturn(Revision.create(givenRev, MOCK_REPO_NAME));
    expect(mockRepo.archiveAtRevision(givenRev)).andReturn(new File(archiveTempDir));

    control.replay();

    Codebase codebase = codebaseCreator.create(ImmutableMap.of("revision", givenRev));

    assertEquals(new File(archiveTempDir), codebase.path());
    assertEquals("public", codebase.projectSpace());
    assertEquals("mockrepo(revision=" + givenRev + ")", codebase.expression().toString());

    control.verify();
  }
}
