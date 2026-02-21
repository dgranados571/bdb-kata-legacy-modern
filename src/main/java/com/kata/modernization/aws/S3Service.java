package com.kata.modernization.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.regions.Region;
import java.time.Duration;

@Service
public class S3Service {

    private final String accessKey;
    private final String secretKey;
    private final String bucketName;
    private final Region region;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3Service(
            @Value("${aws.accessKey}") String accessKey,
            @Value("${aws.secretKey}") String secretKey,
            @Value("${aws.region}") String regionName,
            @Value("${aws.s3.bucket}") String bucketName) {

        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucketName = bucketName;
        this.region = Region.of(regionName);

        System.out.println("Access Key: " + this.accessKey);
        System.out.println("Secret Key: " + this.secretKey);
        System.out.println("Bucket Name: " + this.bucketName);
        System.out.println("Region: " + this.region);

        AwsBasicCredentials credentials = AwsBasicCredentials.create(this.accessKey, this.secretKey);

        this.s3Client = S3Client.builder()
                .region(this.region)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        this.s3Presigner = S3Presigner.builder()
                .region(this.region)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    public void uploadArtifact(String key, String filePath) {
        System.out.println("Uploading artifact to S3: s3://" + this.bucketName + "/" + key);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(this.bucketName)
                .key(key)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromString("Contenido binario simulado del JAR"));

        System.out.println("Upload successful.");
    }

    public String getPresignedUrl(String key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(this.bucketName)
                .key(key)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner
                .presignPutObject(r -> r.signatureDuration(Duration.ofMinutes(10)).putObjectRequest(putObjectRequest));

        return presignedRequest.url().toString();
    }
}
