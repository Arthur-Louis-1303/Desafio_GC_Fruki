package com.fruki.recomenda.repository;

import com.fruki.recomenda.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {}
