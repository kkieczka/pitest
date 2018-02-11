package org.pitest.mutationtest.engine.gregor.mutators.experimental;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;

/**
 * The mutator which enables the parsing of XML DOCTYPE declarations by KXMLParser
 * obtained using following sequence: <br>
 * <code>XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();</code>
 * <br>
 * This mutator sets <code>XmlPullParser.FEATURE_PROCESS_DOCDECL</code> feature to true, as if
 * following code was invoked just after creation of the parser: <br>
 * <code>parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, true);</code>
 * <br>
 * Created by Krystian Kieczka on 2017-12-30.
 */
public enum XMLEnabledDoctypeParsingMutator implements MethodMutatorFactory {

    XML_ENABLED_DOCTYPE_PARSING_MUTATOR;

    private enum State {
        BEFORE_XML_PARSER_CREATION,
        XML_PARSER_CREATED,
        XML_PARSER_STORED
    }

    public class XMLEnabledDoctypeParsingMethodVisitor extends MethodVisitor {

        private MutationContext context;
        private MethodMutatorFactory factory;
        private State state;

        public XMLEnabledDoctypeParsingMethodVisitor(MethodMutatorFactory factory,
                                                     MutationContext context, MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
            this.context = context;
            this.factory = factory;
            this.state = State.BEFORE_XML_PARSER_CREATION;
        }

        /*
            L0:     invokestatic Method org/xmlpull/v1/XmlPullParserFactory newInstance ()Lorg/xmlpull/v1/XmlPullParserFactory;
            L3:     astore_2
            L4:     aload_2
            L5:     invokevirtual Method org/xmlpull/v1/XmlPullParserFactory newPullParser ()Lorg/xmlpull/v1/XmlPullParser;
            L8:     astore_3
            L9:     aload_3
            L10:    ldc 'http://xmlpull.org/v1/doc/features.html#process-docdecl'
            L12:    iconst_1
            L13:    invokeinterface InterfaceMethod org/xmlpull/v1/XmlPullParser setFeature (Ljava/lang/String;Z)V 3
        */

        private boolean isParserCreation(String methodOwner, String methodName) {
            // PITest uses relocation plugin which 'silently' moves certain packages (these
            // used by PITest internally). One of them is org.xmlpull which is changed
            // to org.pitest.reloc.xmlpull. It turns out, that string references to such
            // class names are also somehow changed at some point during build.
            // The construction below is a hack to avoid this behavior.
            String mo = "org";
            mo = mo.concat("/xmlpull/v1/XmlPullParserFactory");

            return mo.equals(methodOwner) && "newPullParser".equals(methodName);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

            if (state != State.BEFORE_XML_PARSER_CREATION) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                return;
            }

            if (opcode == Opcodes.INVOKEVIRTUAL && isParserCreation(owner, name)) {
                state = State.XML_PARSER_CREATED;
            }

            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }

        private void createMutation() {
            // PITest uses relocation plugin which 'silently' moves certain packages (these
            // used by PITest internally). One of them is org.xmlpull which is changed
            // to org.pitest.reloc.xmlpull. It turns out, that string references to such
            // class names are also somehow changed at some point during build.
            // The construction below is a hack to avoid this behavior.
            String className = "org";
            className = className.concat("/xmlpull/v1/XmlPullParser");

            this.mv.visitLdcInsn("http://xmlpull.org/v1/doc/features.html#process-docdecl");
            this.mv.visitInsn(Opcodes.ICONST_1);
            this.mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, className,
                    "setFeature", "(Ljava/lang/String;Z)V", true);

            final MutationIdentifier newId = this.context.registerMutation(
                    this.factory, "Added call to setFeature(FEATURE_PROCESS_DOCDECL, true)");
        }

        @Override
        public void visitVarInsn(int opcode, int varNo) {
            if (state != State.XML_PARSER_CREATED) {
                super.visitVarInsn(opcode, varNo);
                return;
            }

            // assume first astore instruction after parser creation stores the parser
            if (opcode == Opcodes.ASTORE) {
                state = State.XML_PARSER_STORED;

                // pass this store instruction through
                this.mv.visitVarInsn(opcode, varNo);

                // mutate: get the parser object and invoke setFeature() on it
                this.mv.visitVarInsn(Opcodes.ALOAD, varNo);
                createMutation();

            } else {
                super.visitVarInsn(opcode, varNo);
            }
        }

        @Override
        public void visitInsn(int opcode) {
            if (state != State.XML_PARSER_CREATED) {
                super.visitInsn(opcode);
                return;
            }

            // if we are in XML_PARSER_CREATED state and there is a ARETURN, assume it tries to return
            // the parser (otherwise it would neither be assigned to any variable nor would be returned,
            // which would mean that the parser was created, but not intended to be used)
            if (opcode == Opcodes.ARETURN) {
                this.mv.visitInsn(Opcodes.DUP);
                createMutation();
                this.mv.visitInsn(opcode);
                state = State.XML_PARSER_STORED;

            } else {
                super.visitInsn(opcode);

            }
        }
    }

    @Override
    public MethodVisitor create(MutationContext context, MethodInfo methodInfo, MethodVisitor methodVisitor) {
        return new XMLEnabledDoctypeParsingMethodVisitor(this, context, methodVisitor);
    }

    @Override
    public String getGloballyUniqueId() {
        return this.getName();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}

