package ru.chavkin.em.linkshortener.entity.mapper;

import org.mapstruct.Mapper;
import ru.chavkin.em.linkshortener.entity.Link;
import ru.chavkin.em.linkshortener.entity.dto.ShortenResponse;

@Mapper(componentModel = "spring")
public interface LinkMapper {

    ShortenResponse fromEntityToResponse(Link link);

}
