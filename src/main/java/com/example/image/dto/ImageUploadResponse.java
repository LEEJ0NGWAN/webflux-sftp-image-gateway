package com.example.image.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder @Getter @Setter
public class ImageUploadResponse {

    private String location;
    private Boolean result;
}
