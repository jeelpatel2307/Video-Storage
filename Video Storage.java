@SpringBootApplication
public class VideoStorageApp {
    public static void main(String[] args) {
        SpringApplication.run(VideoStorageApp.class, args);
    }
}

@RestController
@RequestMapping("/videos")
public class VideoController {
    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping
    public ResponseEntity<VideoDTO> uploadVideo(@RequestParam("file") MultipartFile file, @RequestParam("title") String title) {
        Video video = videoService.uploadVideo(file, title);
        return ResponseEntity.ok(VideoDTO.fromVideo(video));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoDTO> getVideo(@PathVariable Long id) {
        Video video = videoService.getVideo(id);
        return ResponseEntity.ok(VideoDTO.fromVideo(video));
    }
}

@Service
public class VideoService {
    private final VideoRepository videoRepository;
    private final StorageService storageService;

    public VideoService(VideoRepository videoRepository, StorageService storageService) {
        this.videoRepository = videoRepository;
        this.storageService = storageService;
    }

    public Video uploadVideo(MultipartFile file, String title) {
        String fileUrl = storageService.storeFile(file);
        Video video = new Video(title, fileUrl);
        return videoRepository.save(video);
    }

    public Video getVideo(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
    }
}

@Entity
@Data
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String fileUrl;
    private LocalDateTime uploadedAt;

    public Video(String title, String fileUrl) {
        this.title = title;
        this.fileUrl = fileUrl;
        this.uploadedAt = LocalDateTime.now();
    }
}

@Data
public class VideoDTO {
    private Long id;
    private String title;
    private String fileUrl;
    private LocalDateTime uploadedAt;

    public static VideoDTO fromVideo(Video video) {
        VideoDTO dto = new VideoDTO();
        dto.setId(video.getId());
        dto.setTitle(video.getTitle());
        dto.setFileUrl(video.getFileUrl());
        dto.setUploadedAt(video.getUploadedAt());
        return dto;
    }
}

@Service
public class StorageService {
    private final AmazonS3 s3Client;
    private final String bucketName = "your-bucket-name";

    public StorageService(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }

    public String storeFile(MultipartFile file) {
        String fileName = UUID.randomUUID().toString() + "." + getFileExtension(file.getOriginalFilename());
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(new PutObjectRequest(bucketName, fileName, inputStream, metadata)
                    .withCannedAcl(CannedAccessControlList.PublicRead));
            return getFileUrl(fileName);
        } catch (IOException e) {
            throw new RuntimeException("Error storing file", e);
        }
    }

    public void deleteFile(String fileName) {
        s3Client.deleteObject(bucketName, fileName);
    }

    private String getFileUrl(String fileName) {
        return s3Client.getResourceUrl(bucketName, fileName);
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        return dotIndex > 0 ? fileName.substring(dotIndex + 1) : "";
    }
}

@ControllerAdvice
public class GlobalExceptionHandler {
    // Exception handling logic similar to the previous example
}

public class ResourceNotFoundException extends RuntimeException {
    // Exception class similar to the previous example
}
