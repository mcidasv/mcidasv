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

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.WrapperException;

import edu.wisc.ssec.mcidasv.util.BackgroundTask;

public class Submitter extends BackgroundTask<String> {

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
        if (form.hasAttachmentOne())
            parts.add(buildRealFilePart("form_data[att_two]", form.getAttachmentOne()));
        if (form.hasAttachmentTwo())
            parts.add(buildRealFilePart("form_data[att_three]", form.getAttachmentTwo()));

        // if the user wants, attach an XML bundle of the state
        if (form.canBundleState() && form.getSendBundle())
            parts.add(buildFakeFilePart("form_data[att_state]", form.getBundledStateName(), form.getBundledState()));

        // attach system properties
        parts.add(buildFakeFilePart("form_data[att_extra]", form.getExtraStateName(), form.getExtraState()));

        if (form.canSendLog())
            parts.add(buildRealFilePart("form_data[att_log]", form.getLogPath()));

        Part[] arr = parts.toArray(new Part[0]);

        MultipartRequestEntity mpr = new MultipartRequestEntity(arr, method.getParams());
        method.setRequestEntity(mpr);
        return method;
    }

    protected String compute() {
        // logic ripped from the IDV's HttpFormEntry#doPost(List, String)
        try {
            while ((tryCount++ < POST_ATTEMPTS) && !isCancelled()) {
                method = buildPostMethod(validFormUrl, form);
                HttpClient client = new HttpClient();
                client.executeMethod(method);
                if (method.getStatusCode() >= 300 && method.getStatusCode() <= 399) {
                    Header location = method.getResponseHeader("location");
                    if (location == null)
                        return "Error: No 'location' given on the redirect";

                    validFormUrl = location.getValue();
                    if (method.getStatusCode() == 301) {
                        System.err.println("Warning: form post has been permanently moved to:" + validFormUrl);
                    }
                    continue;
                }
                break;
            }
            return IOUtil.readContents(method.getResponseBodyAsStream());
        } catch (Exception e) {
            throw new WrapperException("doing post", e);
        }
    }

    @Override protected void onCompletion(String result, Throwable exception, boolean cancelled) {
//        System.err.println("onCompletion: result="+result+" exception="+exception+" cancelled="+cancelled);
        if (cancelled)
            return;

        if (exception == null)
            form.showSuccess();
        else
            form.showFailure(exception.getMessage());
    }

}
