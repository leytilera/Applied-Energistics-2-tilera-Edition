package appeng.transformer.asm.repairers;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import appeng.transformer.asm.InterfaceMethodRepairer;

public class ConstructorRepairer extends InterfaceMethodRepairer {

    static final String init = "<init>";
    static final String storageChannelNew = "Lappeng/api/storage/IStorageChannel;";
    static final String storageChannelOld = "Lappeng/api/storage/StorageChannel;";

    Set<String> constructors = new HashSet<>();
    Set<String> existingPatches = new HashSet<>();

    public ConstructorRepairer(String className) {
            super(className);
    }
    
    @Override
    public void visitMethod(String name, String desc) {
        if (init.equals(name) && desc.contains(storageChannelNew) && !desc.contains(storageChannelOld)) {
            constructors.add(desc);
        } else if (init.equals(name) && desc.contains(storageChannelOld) && !desc.contains(storageChannelNew)) {
            existingPatches.add(desc);
        }
    }

    @Override
    public void generateMethods(ClassVisitor cv) {
        for (String constructorDesc : constructors) {
            String oldDesc = constructorDesc.replace(storageChannelNew, storageChannelOld);
            if (!existingPatches.contains(oldDesc)) {
                Type[] args = Type.getArgumentTypes(constructorDesc);
                MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, init, oldDesc, null, null);
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                for (int i = 1; i<=args.length; i++) {
                    Type type = args[i-1];
                    mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), i);
                }
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, thisClass, init, constructorDesc, false);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        }
    }
    
}
