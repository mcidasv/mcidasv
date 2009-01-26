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

    private static final int POST_ATTEMPTS = 5;
    
    private final SupportForm form;

    private final String requestUrl = "http://www.ssec.wisc.edu/mcidas/misc/mc-v/supportreq/formtest.php";

    private String validFormUrl = requestUrl;

    private int tryCount = 0;

    private PostMethod method = null;

    public Submitter(final SupportForm form) {
        this.form = form;
    }

    private static FilePart buildRealFilePart(final String name, final String file) {
        return new FilePart(name, new PartSource() {
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

    private static FilePart buildFakeFilePart(final String name, final String file, final byte[] data) {
        return new FilePart(name, new PartSource() {
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

        if (form.hasAttachmentOne())
            parts.add(buildRealFilePart("form_data[att_two]", form.getAttachmentOne()));
        if (form.hasAttachmentTwo())
            parts.add(buildRealFilePart("form_data[att_three]", form.getAttachmentTwo()));

        if (form.canBundleState() && form.getSendBundle())
            parts.add(buildFakeFilePart("form_data[att_state]", form.getBundledStateName(), form.getBundledState()));

        parts.add(buildFakeFilePart("form_data[att_extra]", form.getExtraStateName(), form.getExtraState()));

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
        System.err.println("onCompletion: result="+result+" exception="+exception+" cancelled="+cancelled);
    }

}
