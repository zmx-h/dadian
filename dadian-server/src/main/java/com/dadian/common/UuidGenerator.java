package com.dadian.common;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class UuidGenerator implements IdentifierGenerator {
    @Override
    public Number nextId(Object entity) { return null; }
    @Override
    public String nextUUID(Object entity) { return UUID.randomUUID().toString(); }
}
