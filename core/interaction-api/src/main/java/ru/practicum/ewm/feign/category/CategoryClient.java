package ru.practicum.ewm.feign.category;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.ewm.core.exception.ConditionsException;
import ru.practicum.ewm.dto.category.CategoryDto;

import java.util.Map;
import java.util.Set;

@FeignClient(name = "main-service", fallbackFactory = CategoryClientFallbackFactory.class)
public interface CategoryClient {
    @GetMapping("/categories/{id}")
    CategoryDto getCategoryById(@PathVariable Long id) throws ConditionsException;

    @GetMapping("/categories/by-ids")
    Map<Long, CategoryDto> findAllByIds(@RequestParam Set<Long> ids) throws ConditionsException;
}
