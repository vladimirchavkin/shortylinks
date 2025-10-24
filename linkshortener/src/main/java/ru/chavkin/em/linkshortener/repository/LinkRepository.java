package ru.chavkin.em.linkshortener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.chavkin.em.linkshortener.entity.Link;

import java.util.Optional;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {

    Optional<Link> findByShortCode(String shortCode);

    Optional<Link> findByAlias(String alias);

    boolean existsByShortCode(String shortCode);

    boolean existsByAlias(String alias);

}
