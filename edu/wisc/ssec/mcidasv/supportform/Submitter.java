/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */
package edu.wisc.ssec.mcidasv.supportform;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.PartSource;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.util.IOUtil;
//import ucar.unidata.util.Misc;
import ucar.unidata.util.WrapperException;

import edu.wisc.ssec.mcidasv.util.BackgroundTask;

/**
 * Abstraction of a background thread that is used to submit support requests
 * to the McIDAS-V Help Desk Team.
 */
public class Submitter extends BackgroundTask<String> {

    /** Error message to display if the server had problems. */
    public static final String POST_ERROR = "Server encountered an error while attempting to forward message to mug@ssec.wisc.edu.\n\nPlease try sending email in your email client to mug@ssec.wisc.edu. We apologize for the inconvenience.";

    /** Logging object. */
    private static final Logger logger = LoggerFactory.getLogger(Submitter.class);

    /** We'll follow up to this many redirects for {@code requestUrl}. */
    private static final int POST_ATTEMPTS = 5;

    /** Used to gather user input and system information. */
    private final SupportForm form;

    /** URL that we'll attempt to {@code POST} our requests at.*/
    private final String requestUrl = "http://www.ssec.wisc.edu/mcidas/misc/mc-v/supportreq/support.php";

    /** Keeps track of the most recent redirect for {@code requestUrl}. */
    private String validFormUrl = requestUrl;

    /** Number of redirects we've tried since starting. */
    private int tryCount = 0;

    /** Handy reference to the status code (and more) of our {@code POST}. */
    private PostMethod method = null;

    /**
     * Prepare a support request to be sent (off of the event dispatch thread).
     * 
     * @param form Support request form to send. Cannot be {@code null}.
     */
    public Submitter(final SupportForm form) {
        this.form = form;
    }

    /** 
     * Creates a file attachment that's based upon a real file.
     * 
     * @param id The parameter ID. Usually something like 
     * {@literal "form_data[att_two]"}.
     * @param file Path to the file that's going to be attached.
     * 
     * @return {@code POST}-able file attachment using the name and contents of
     * {@code file}.
     */
    private static FilePart buildRealFilePart(final String id, final String file) {
        return new FilePart(id, new PartSource() {
            public InputStream createInputStream() {
                try {
                    return IOUtil.getInputStream(file);
                } catch (Exception e) {
                    throw new WrapperException("Reading file: "+file, e);
                }
            }
            public String getFileName() {
                return new File(file).getName();
            }
            public long getLength() {
                return new File(file).length();
            }
        });
    }

    /**
     * Creates a file attachment that isn't based upon an actual file. Useful 
     * for something like the {@literal "extra"} attachment where you collect
     * a bunch of data but don't want to deal with creating a temporary file.
     * 
     * @param id Parameter ID. Typically something like 
     * {@literal "form_data[att_extra]"}.
     * @param file Fake name of the file. Can be whatever you like.
     * @param data The actual data to place inside the attachment.
     * 
     * @return {@code POST}-able file attachment using a spoofed filename!
     */
    private static FilePart buildFakeFilePart(final String id, final String file, final byte[] data) {
        return new FilePart(id, new PartSource() {
            public InputStream createInputStream() {
                return new ByteArrayInputStream(data);
            }
            public String getFileName() {
                return file;
            }
            public long getLength() {
                return data.length;
            }
        });
    }

    /**
     * Attempts to {@code POST} to {@code url} using the information from 
     * {@code form}.
     * 
     * @param url URL that'll accept the {@code POST}. Typically 
     * {@link #requestUrl}.
     * @param form The {@link SupportForm} that contains the data to use in the
     * support request.
     * 
     * @return Big honkin' object that contains the support request.
     */
    private static PostMethod buildPostMethod(String url, SupportForm form) {
        PostMethod method = new PostMethod(url);

        List<Part> parts = new ArrayList<Part>();
        parts.add(new StringPart("form_data[fromName]", form.getUser()));
        parts.add(new StringPart("form_data[email]", form.getEmail()));
        parts.add(new StringPart("form_data[organization]", form.getOrganization()));
        parts.add(new StringPart("form_data[subject]", form.getSubject()));
        parts.add(new StringPart("form_data[description]", form.getDescription()));
        parts.add(new StringPart("form_data[submit]", ""));
        parts.add(new StringPart("form_data[p_version]", "p_version=ignored"));
        parts.add(new StringPart("form_data[opsys]", "opsys=ignored"));
        parts.add(new StringPart("form_data[hardware]", "hardware=ignored"));
        parts.add(new StringPart("form_data[cc_user]", Boolean.toString(form.getSendCopy())));

        // attach the files the user has explicitly attached.
        if (form.hasAttachmentOne()) {
            parts.add(buildRealFilePart("form_data[att_two]", form.getAttachmentOne()));
        }
        if (form.hasAttachmentTwo()) {
            parts.add(buildRealFilePart("form_data[att_three]", form.getAttachmentTwo()));
        }
        // if the user wants, attach an XML bundle of the state
        if (form.canBundleState() && form.getSendBundle()) {
            parts.add(buildFakeFilePart("form_data[att_state]", form.getBundledStateName(), form.getBundledState()));
        }

        // attach system properties
        parts.add(buildFakeFilePart("form_data[att_extra]", form.getExtraStateName(), form.getExtraState()));

        if (form.canSendLog()) {
            parts.add(buildRealFilePart("form_data[att_log]", form.getLogPath()));
        }

        Part[] arr = parts.toArray(new Part[0]);
        MultipartRequestEntity mpr = new MultipartRequestEntity(arr, method.getParams());
        method.setRequestEntity(mpr);
        return method;
    }

    /**
     * Attempt to POST contents of support request form to {@link #requestUrl}.
     * 
     * @throws WrapperException if there was a problem on the server.
     */
    protected String compute() {
        // logic ripped from the IDV's HttpFormEntry#doPost(List, String)
        try {
            while ((tryCount++ < POST_ATTEMPTS) && !isCancelled()) {
                method = buildPostMethod(validFormUrl, form);
                HttpClient client = new HttpClient();
                client.executeMethod(method);
                if (method.getStatusCode() >= 300 && method.getStatusCode() <= 399) {
                    Header location = method.getResponseHeader("location");
                    if (location == null) {
                        return "Error: No 'location' given on the redirect";
                    }
                    validFormUrl = location.getValue();
                    if (method.getStatusCode() == 301) {
                        logger.warn("form post has been permanently moved to: {}", validFormUrl);
                    }
                    continue;
                }
                break;
            }
            return IOUtil.readContents(method.getResponseBodyAsStream());
        } catch (Exception e) {
            throw new WrapperException(POST_ERROR, e);
        }
    }

//    protected String compute() {
//        try {
//            Misc.sleep(2000);
//            return "dummy success!";
//        } catch (Exception e) {
//            throw new WrapperException(POST_ERROR, e);
//        }
//    }

    /**
     * Handles completion of a support request.
     * 
     * @param result Result of {@link #compute()}.
     * @param exception Exception thrown from {@link #compute()}, if any.
     * @param cancelled Whether or not the user opted to cancel.
     */
    @Override protected void onCompletion(String result, Throwable exception, boolean cancelled) {
        logger.trace("result={} exception={} cancelled={}", new Object[] { result, exception, cancelled });
        if (cancelled) {
            return;
        }

        if (exception == null) {
            form.showSuccess();
        } else {
            form.showFailure(exception.getMessage());
        }
    }

}
