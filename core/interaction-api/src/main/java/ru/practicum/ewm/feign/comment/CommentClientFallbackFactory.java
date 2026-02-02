package ru.practicum.ewm.feign.comment;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class CommentClientFallbackFactory
        implements FallbackFactory<CommentClient> {

    @Override
    public CommentClient create(Throwable cause) {
        if (cause instanceof FeignException fe) {
            int status = fe.status();

            if (status == 404 || status == 409) {
                return id -> {
                    throw fe;
                };
            }
        }

        return id -> Collections.emptyList();
    }
}