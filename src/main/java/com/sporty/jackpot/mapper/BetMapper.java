package com.sporty.jackpot.mapper;

import com.sporty.jackpot.dto.BetMessage;
import com.sporty.jackpot.dto.BetRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BetMapper {
    @Mapping(target = "betId", source = "betId")
    BetMessage toMessage(BetRequest request, UUID betId);
}
