package com.example.image.vo;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class ImageUploadRequest {

    private String payload; // base64 encoded image file payload
    private String name; // image file name
}
