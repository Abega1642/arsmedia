package dev.razafindratelo.arsmedia.model;

import dev.razafindratelo.arsmedia.model.classifier.FileType;
import dev.razafindratelo.arsmedia.model.classifier.SizeType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Media {
  private String id;
  private String fileName;
  private double size;
  private SizeType sizeType;
  private FileType fileType;
  private LocalDateTime createdAt;
  private String filePath;
  private User owner;
}
