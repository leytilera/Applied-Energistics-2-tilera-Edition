package appeng.transformer.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import net.minecraft.launchwrapper.IClassTransformer;

public class StorageChannelTweaks implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!transformedName.equals("appeng.api.storage.StorageChannel")) {
            return basicClass;
        }
        ClassReader classReader = new ClassReader(basicClass);
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM5, classWriter) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                String[] interf = new String[]{
                    "appeng/api/storage/channels/IItemStorageChannel",
                    "appeng/api/storage/channels/IFluidStorageChannel"
                };
                super.visit(version, access, name, signature, superName, interf);
            }
        };
        classReader.accept(classVisitor, 0);
        return classWriter.toByteArray();
    }
    
}
