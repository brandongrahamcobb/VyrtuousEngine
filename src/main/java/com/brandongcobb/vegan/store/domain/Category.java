package com.brandongcobb.vegan.store.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    public Category() { }

    public Category(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    // No setter for id—JPA manages this

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
