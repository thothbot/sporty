package com.sporty.jackpot.mapper;

import com.sporty.jackpot.dto.JackpotDto;
import com.sporty.jackpot.entity.Jackpot;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface JackpotMapper {
    JackpotDto toDto(Jackpot jackpot);
    Jackpot toEntity(JackpotDto dto);
}
