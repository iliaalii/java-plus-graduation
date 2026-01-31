package ru.practicum.ewm.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.service.CategoryService;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping(path = "/categories")
@RequiredArgsConstructor
@Validated
@Slf4j
public class CategoryController {
    private final CategoryService service;

    @GetMapping
    public List<CategoryDto> find(@PageableDefault(page = 0, size = 10) Pageable pageable) {
        return service.find(pageable);
    }

    @GetMapping("/{catId}")
    public CategoryDto findById(@Positive @PathVariable Long catId) {
        return service.getEntity(catId);
    }

    @GetMapping("/by-ids")
    public Map<Long, CategoryDto> findAllByIds(@RequestParam Set<Long> ids) {
        return service.findAllByIds(ids);
    }
}
