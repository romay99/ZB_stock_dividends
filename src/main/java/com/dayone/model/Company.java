package com.dayone.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Company {

    private String ticker;
    private String name;

}
