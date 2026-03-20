package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Category;
import com.ecommerce.project.payload.CategoryDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Category findByCategoryName(String categoryName);

    void delete(Category category);
}
