package io.github.vampireachao.vespera.core.world;

import io.github.vampireachao.vespera.core.voxel.IsoRenderer;
import io.github.vampireachao.vespera.core.voxel.Scene;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class R {
    @Test
    void r() throws Exception {
        Scene s = MedievalCity.build();
        IsoRenderer.save(IsoRenderer.render(s, 120, -40, 200, 40, 1500, 1100, 14.0, "东大道"),
                Path.of("/tmp/e_ave.png"));
        IsoRenderer.save(IsoRenderer.render(s, -60, 190, 40, 290, 1500, 1100, 14.0, "南市街"),
                Path.of("/tmp/s_street.png"));
    }
}
