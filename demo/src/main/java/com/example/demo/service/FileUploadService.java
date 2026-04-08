package com.example.demo.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

@Service
public class FileUploadService {
    @Autowired
    private Cloudinary cloudinary;

    public String uploadFile(MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "auto",
                        "folder", "CDCM",   // ✅ Explicitly set the folder name
                        "type", "upload",          // ✅ Ensure it is a public 'upload' type
                        "access_mode", "public"    // ✅ Explicitly set to public
                ));
        return uploadResult.get("secure_url").toString();
    }
}