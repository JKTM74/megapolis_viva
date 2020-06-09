package com.megapolis.viva.api.v1.connector;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

public class ThesisRestTemplate extends RestTemplate {

    /*в общем в этом методе изначаьлно твориться какая-то чушь и он изменяет uri,
    в итоге изменяется запрос и в тезис шлётся не понятно что
    поэтому метод был слегка видоизменён, теперь всё работает
     */
    @Override
    public <T> T execute(String url, HttpMethod method, RequestCallback requestCallback, ResponseExtractor<T> responseExtractor, Object... uriVariables) throws RestClientException {
        URI uri = URI.create(url);
        return doExecute(uri, method, requestCallback, responseExtractor);
    }
}
