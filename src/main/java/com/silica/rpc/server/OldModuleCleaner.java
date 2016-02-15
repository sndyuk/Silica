/**
 *    Copyright (C) 2011-2016 sndyuk
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.silica.rpc.server;

import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.silica.Silica;
import com.silica.job.Job;
import com.silica.job.JobException;

/**
 * Old module cleaning job for framework
 * 
 * @author sndyuk
 */
public class OldModuleCleaner implements Job<Boolean> {
    private static final Logger LOG = LoggerFactory.getLogger(OldModuleCleaner.class);
    private static final long serialVersionUID = 1L;

    public OldModuleCleaner() {
    }

    @Override
    public Boolean execute() throws JobException {

        String basdir = Silica.getResourceDirectory();
        File root = new File(basdir).getParentFile();
        LOG.info("cleaning old modules in {}", basdir);

        if (!root.isDirectory()) {
            throw new JobException(MessageFormat.format("Could not found base directory {0}", basdir));
        }

        String[] sortedFileNames = root.list();
        Arrays.sort(sortedFileNames);
        boolean cleaned = true;
        for (int i = sortedFileNames.length - 1 - Silica.getNumOfKeepDeployed(); i >= 0; i--) {
            File f = new File(root, sortedFileNames[i]);
            if (!f.getAbsolutePath().contains("silica")) { // Just in case.
                throw new JobException("Do not remove the folder " + f.getAbsolutePath() + ". File path must contains 'silica'.");
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug(MessageFormat.format("Cleaning file: {0}", f.getAbsolutePath()));
            }
            if (!deleteChildren(f)) {
                LOG.error(MessageFormat.format("Could not clean file: {0}", f.getAbsolutePath()));
                cleaned = false;
            }
        }
        return Boolean.valueOf(cleaned);
    }

    private static boolean deleteChildren(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                deleteChildren(files[i]);
            }
        }
        return file.delete();
    }
}
