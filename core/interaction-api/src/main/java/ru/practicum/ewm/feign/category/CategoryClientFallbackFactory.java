package ru.practicum.ewm.feign.category;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.core.exception.ConditionsException;
import ru.practicum.ewm.dto.category.CategoryDto;

import java.util.Map;
import java.util.Set;

@Component
public class CategoryClientFallbackFactory implements FallbackFactory<CategoryClient> {

    @Override
    public CategoryClient create(Throwable cause) {
        if (cause instanceof FeignException fe) {
            int status = fe.status();
            if (status == 409 || status == 404) {
                return new CategoryClient() {
                    @Override
                    public CategoryDto getCategoryById(Long id) {
                        throw fe;
                    }

                    @Override
                    public Map<Long, CategoryDto> findAllByIds(Set<Long> ids) {
                        throw fe;
                    }
                };
            }
        }

        return new CategoryClient() {
            @Override
            public CategoryDto getCategoryById(Long id) throws ConditionsException {
                throw new ConditionsException("category-service недоступен");
            }

            @Override
            public Map<Long, CategoryDto> findAllByIds(Set<Long> ids) throws ConditionsException {
                throw new ConditionsException("category-service недоступен");
            }
        };
    }
}
