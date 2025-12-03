package ru.chavkin.em.linkshortener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.chavkin.em.linkshortener.entity.Link;

import java.util.Optional;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {

    @Query("SELECT l FROM Link l WHERE l.alias = :code OR l.shortCode = :code")
    Optional<Link> findByAliasOrShortCode(@Param("code") String code);

    Optional<Link> findByShortCode(String shortCode);

    Optional<Link> findByAlias(String alias);

    boolean existsByShortCode(String shortCode);

    boolean existsByAlias(String alias);

}
