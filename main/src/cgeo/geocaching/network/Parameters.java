package cgeo.geocaching.network;

import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * List of key/values pairs to be used in a GET or POST request.
 *
 */
public class Parameters extends ArrayList<ImmutablePair<String, String>> {

    private static final long serialVersionUID = 1L;
    private boolean percentEncoding = false;

    /**
     * @param keyValues
     *            list of initial key/value pairs
     * @throws InvalidParameterException
     *             if the number of key/values is unbalanced
     */
    public Parameters(final String... keyValues) {
        put(keyValues);
    }

    private static final Comparator<ImmutablePair<String, String>> comparator = new Comparator<ImmutablePair<String, String>>() {
        @Override
        public int compare(final ImmutablePair<String, String> nv1, final ImmutablePair<String, String> nv2) {
            final int comparedKeys = nv1.left.compareTo(nv2.left);
            return comparedKeys != 0 ? comparedKeys : nv1.right.compareTo(nv2.right);
        }
    };

    /**
     * Percent encode following http://tools.ietf.org/html/rfc5849#section-3.6
     */
    static String percentEncode(@NonNull final String url) {
        return StringUtils.replace(Network.rfc3986URLEncode(url), "*", "%2A");
    }

    /**
     * Add new key/value pairs to the current parameters.
     *
     * @param keyValues
     *            list of key/value pairs
     * @throws InvalidParameterException
     *             if the number of key/values is unbalanced
     * @return the object itself to facilitate chaining
     */
    public Parameters put(final String... keyValues) {
        if (keyValues.length % 2 == 1) {
            throw new InvalidParameterException("odd number of parameters");
        }
        for (int i = 0; i < keyValues.length; i += 2) {
            add(ImmutablePair.of(keyValues[i], keyValues[i + 1]));
        }
        return this;
    }

    /**
     * Lexically sort key/value pairs first by key, then by value.
     *
     * Some signing algorithms need the values to be ordered before issuing the signature.
     */
    public void sort() {
        Collections.sort(this, comparator);
    }

    /**
     * Some sites require the use of percent encoding (see {@link #percentEncode(String)}) and do not
     * accept other encodings during their authorization and signing processes. This forces those
     * parameters to use percent encoding instead of the regular encoding.
     */
    public void usePercentEncoding() {
        percentEncoding = true;
    }

    @Override
    public String toString() {
        if (percentEncoding) {
            if (isEmpty())
                return "";
            final StringBuilder builder = new StringBuilder();
            for (final ImmutablePair<String, String> nameValuePair : this) {
                builder.append('&').append(percentEncode(nameValuePair.left)).append('=').append(percentEncode(nameValuePair.right));
            }
            return builder.substring(1);
        } else {
            final Builder builder = HttpUrl.parse("http://dummy.cgeo.org/").newBuilder();
            for (final ImmutablePair<String, String> nameValuePair : this) {
                builder.addQueryParameter(nameValuePair.left, nameValuePair.right);
            }
            return StringUtils.defaultString(builder.build().encodedQuery());
        }
    }

    /**
     * Extend or create a Parameters object with new key/value pairs.
     *
     * @param params
     *            an existing object or null to create a new one
     * @param keyValues
     *            list of key/value pair
     * @throws InvalidParameterException
     *             if the number of key/values is unbalanced
     * @return the object itself if it is non-null, a new one otherwise
     */
    @NonNull
    public static Parameters extend(@Nullable final Parameters params, final String... keyValues) {
        return params == null ? new Parameters(keyValues) : params.put(keyValues);
    }

    /**
     * Merge two (possibly null) Parameters object.
     *
     * @param params
     *            the object to merge into if non-null
     * @param extra
     *            the object to merge from if non-null
     * @return params with extra data if params was non-null, extra otherwise
     */
    @Nullable
    public static Parameters merge(@Nullable final Parameters params, @Nullable final Parameters extra) {
        if (params == null) {
            return extra;
        }
        if (extra != null) {
            params.addAll(extra);
        }
        return params;
    }

    public void add(final String key, final String value) {
        put(key, value);
    }

}
