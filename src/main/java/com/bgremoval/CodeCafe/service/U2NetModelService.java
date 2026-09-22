package com.bgremoval.CodeCafe.service;

import com.bgremoval.CodeCafe.exception.ModelException;

import java.awt.image.BufferedImage;

/**
 * Contract for running U2Net ONNX inference on a source image.
 *
 * <p>Implementations are expected to be Spring singletons that load the ONNX
 * model once at startup (via {@code @PostConstruct}) and expose inference via
 * this method.
 */
public interface U2NetModelService {

    /**
     * Runs U2Net inference on the given image.
     *
     * @param image source image (any size; will be resized internally to 320×320)
     * @return a 2-D float array {@code [320][320]} of saliency/alpha values in
     *         {@code [0, 1]}; caller is responsible for upsampling to the original
     *         image dimensions before applying as an alpha mask
     * @throws ModelException if inference fails (e.g. ORT error, null input)
     */
    float[][] predict(BufferedImage image) throws ModelException;
}
