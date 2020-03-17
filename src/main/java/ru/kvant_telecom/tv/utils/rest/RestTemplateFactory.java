package ru.kvant_telecom.tv.utils.rest;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;

public class RestTemplateFactory {


    /**
     * Custom header, contains request (last redirected) URL - allow get redirect url
     */
    public static final String X_LAST_URI = "X-LastURL";
    private static final int TIMEOUT = 5000;
    private static final boolean CHECK_CERT = true;
    private static final boolean THROW_ON_ERROR = true;



    public static RestTemplate getRestTemplate() {

        return getRestTemplateInternal(CHECK_CERT, THROW_ON_ERROR, TIMEOUT);
    }

    public static RestTemplate getRestTemplate(int timeout) {

        return getRestTemplateInternal(CHECK_CERT, THROW_ON_ERROR, timeout);
    }

    public static RestTemplate getRestTemplate(boolean throwOnError, int timeout) {

        return  getRestTemplateInternal(CHECK_CERT, throwOnError, timeout);
    }


    public static RestTemplate getRestTemplate(boolean checkCert, boolean throwOnError) {

        return  getRestTemplateInternal(checkCert, throwOnError, TIMEOUT);
    }

    public static RestTemplate getRestTemplate(boolean checkCert, boolean throwOnError, int timeout) {

        return  getRestTemplateInternal(checkCert, throwOnError, timeout);
    }


    

    private static RestTemplate getRestTemplateInternal(boolean checkCert, boolean throwOnError, int timeout) {


        RestTemplate result;


        RedirectStrategy redirectStrategy = new DefaultRedirectStrategy() {

            //private String lastRedirect = null;

            @Override
            public boolean isRedirected(HttpRequest request, HttpResponse response,
                                        HttpContext context) throws ProtocolException {

                boolean isRedirected;
                //System.out.println(response);

                isRedirected = super.isRedirected(request, response, context);

//                // If redirect intercept intermediate response.
//                if (isRedirected) {
//                    int statusCode  = response.getStatusLine().getStatusCode();
//                    System.out.println("redirectURL: " + redirectURL);
//                    lastRedirect = response.getFirstHeader("Location").getValue();
//                }
//                else {
//                    if (lastRedirect != null) {
//                        response.setHeader(X_LAST_URI, lastRedirect);
//                    }
//
//                }

                // If redirected then add X_LAST_URI header
                response.setHeader(X_LAST_URI, request.getRequestLine().getUri());

                return isRedirected;
            }
        };


        if (!checkCert) {


            try {

                TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

                SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                        .loadTrustMaterial(null, acceptingTrustStrategy)
                        .build();


                RequestConfig requestConfig = RequestConfig.custom()
                        .setConnectTimeout(timeout)
                        .setConnectionRequestTimeout(timeout)
                        .setSocketTimeout(timeout)
                        .build();


                SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
                
                CloseableHttpClient httpClient = HttpClients.custom()
                        .setDefaultRequestConfig(requestConfig)
                        .setSSLSocketFactory(csf)
                        .setRedirectStrategy(redirectStrategy)
                        .build();



                HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();

                requestFactory.setConnectTimeout(timeout);
                requestFactory.setConnectionRequestTimeout(timeout);
                requestFactory.setReadTimeout(timeout);
                requestFactory.setHttpClient(httpClient);

                result = new RestTemplate(requestFactory);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        else {

            CloseableHttpClient httpClient = HttpClients.custom()
                    .setRedirectStrategy(redirectStrategy)
                    .build();
            
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setConnectionRequestTimeout(timeout);
            requestFactory.setConnectTimeout(timeout);
            requestFactory.setReadTimeout(timeout);
            requestFactory.setHttpClient(httpClient);

            result = new RestTemplate(requestFactory);
        }

        // Force using UTF-8 as default character encoding
        result.getMessageConverters()
                .add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));

        // Add custom errorHandler that suppress any exceptions
        if (!throwOnError) {
            result.setErrorHandler(new RestTemplateResponseErrorHandler());
        }

        return result;
    }

}
