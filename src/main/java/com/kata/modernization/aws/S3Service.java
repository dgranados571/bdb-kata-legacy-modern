package com.kata.modernization.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.regions.Region;
import java.time.Duration;
import java.util.List;

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

                this.accessKey = accessKey.trim();
                this.secretKey = secretKey.trim();
                this.bucketName = bucketName.trim();
                this.region = Region.of(regionName.trim());

                AwsBasicCredentials credentials = AwsBasicCredentials.create(this.accessKey, this.secretKey);

                this.s3Client = S3Client.builder()
                                .region(this.region)
                                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                                .httpClientBuilder(software.amazon.awssdk.http.apache.ApacheHttpClient.builder())
                                .build();

                this.s3Presigner = S3Presigner.builder()
                                .region(this.region)
                                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                                .build();
        }

        public String getPresignedUrl(String key) {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                .bucket(this.bucketName)
                                .key(key)
                                .build();

                PresignedPutObjectRequest presignedRequest = s3Presigner
                                .presignPutObject(r -> r.signatureDuration(Duration.ofMinutes(10))
                                                .putObjectRequest(putObjectRequest));

                return presignedRequest.url().toString();
        }

        public String getObjectContent(String key) {
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                .bucket(this.bucketName)
                                .key(key)
                                .build();

                return s3Client.getObject(getObjectRequest, ResponseTransformer.toBytes()).asUtf8String();
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

        public List<String> listBuckets() {
                return s3Client.listBuckets().buckets().stream()
                                .map(b -> b.name())
                                .toList();
        }

        public List<String> listObjects(String prefix) {
                software.amazon.awssdk.services.s3.model.ListObjectsV2Request request = software.amazon.awssdk.services.s3.model.ListObjectsV2Request
                                .builder()
                                .bucket(this.bucketName)
                                .prefix(prefix)
                                .build();
                return s3Client.listObjectsV2(request).contents().stream()
                                .map(o -> o.key())
                                .toList();
        }

}
