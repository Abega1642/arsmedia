package dev.razafindratelo.arsmedia.service.media;

import java.io.File;
import java.util.function.Function;

public interface MediaMetadataExtractor<T> extends Function<File, T> {}
