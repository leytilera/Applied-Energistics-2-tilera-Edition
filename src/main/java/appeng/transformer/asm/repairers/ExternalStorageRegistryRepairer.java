package appeng.transformer.asm.repairers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import appeng.transformer.asm.InterfaceMethodRepairer;

public class ExternalStorageRegistryRepairer extends InterfaceMethodRepairer {

    static final String methGH = "getHandler";
    static final String descGHNew = "(Lnet/minecraft/tileentity/TileEntity;Lnet/minecraftforge/common/util/ForgeDirection;Lappeng/api/storage/IStorageChannel;Lappeng/api/networking/security/BaseActionSource;)Lappeng/api/storage/IExternalStorageHandler;";
    static final String descGHOld = "(Lnet/minecraft/tileentity/TileEntity;Lnet/minecraftforge/common/util/ForgeDirection;Lappeng/api/storage/StorageChannel;Lappeng/api/networking/security/BaseActionSource;)Lappeng/api/storage/IExternalStorageHandler;";

    public ExternalStorageRegistryRepairer(String className) {
        super(className);
    }

    @Override
    public void visitMethod(String name, String desc) {
        
    }

    @Override
    public void generateMethods(ClassVisitor cv) {
        if ("appeng/api/storage/IExternalStorageRegistry".equals(thisClass)) {
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methGH, descGHOld, null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitVarInsn(Opcodes.ALOAD, 4);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, thisClass, methGH, descGHNew, true);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }
    
}
