package seedu.address.logic.commands;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.PASSWORD_1_VALID;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.assertMergeCommandSuccess;
import static seedu.address.logic.commands.DecryptCommand.MESSAGE_DECRYPT_PDF_FAILURE;
import static seedu.address.logic.commands.EncryptCommand.ENCRYPTION_KEY_LENGTH;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PDF;
import static seedu.address.testutil.TypicalIndexes.INDEX_SECOND_PDF;
import static seedu.address.testutil.TypicalIndexes.INDEX_THIRD_PDF;
import static seedu.address.testutil.TypicalPdfs.SAMPLE_PDF_1;
import static seedu.address.testutil.TypicalPdfs.SAMPLE_PDF_2;
import static seedu.address.testutil.TypicalPdfs.SAMPLE_PDF_MERGED_1_2;
import static seedu.address.testutil.TypicalPdfs.getTypicalPdfBook;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import seedu.address.commons.core.Messages;
import seedu.address.commons.core.index.Index;
import seedu.address.logic.CommandHistory;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.PdfBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.pdf.Pdf;
import seedu.address.testutil.PdfBuilder;

public class MergeCommandTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Model model = new ModelManager(getTypicalPdfBook(), new UserPrefs());
    private CommandHistory commandHistory = new CommandHistory();

    private static final String MESSAGE_MERGE_PDF_SUCCESS = "Merged PDFs into new PDF: %1$s";
    private static final String MESSAGE_MERGE_PDF_ENCRYPT = "One or more of selected PDFs is encrypted.";

    @Before
    public void before() throws CommandException {
        decryptPdf(SAMPLE_PDF_1);
        decryptPdf(SAMPLE_PDF_2);
    }

    @Test
    public void constructor_invalidIndex_throwsIndexOutOfBoundsException() {
        thrown.expect(IndexOutOfBoundsException.class);
        new MergeCommand(Index.fromZeroBased(-1), Index.fromZeroBased(1));
    }

    @Test
    public void constructor_nullIndex_throwsIndexOutOfBoundsException() {
        thrown.expect(NullPointerException.class);
        new MergeCommand(null);
    }

    @Test
    public void execute_onlyCompulsoryFieldSpecifiedUnfilteredList_success() throws CommandException{
        Model expectedModel = new ModelManager(new PdfBook(model.getPdfBook()), new UserPrefs());
        MergeCommand mergeCommand = new MergeCommand(INDEX_FIRST_PDF, INDEX_SECOND_PDF);
        CommandResult result = mergeCommand.execute(model, commandHistory);
        String expectedMessage = String.format(MESSAGE_MERGE_PDF_SUCCESS,
                model.getFilteredPdfList().get(3));
        Pdf newMergedPdf = model.getFilteredPdfList().get(3);

        Pdf pdfToTest = new PdfBuilder(SAMPLE_PDF_MERGED_1_2)
                .withName(newMergedPdf.getName().getFullName())
                .withDirectory(newMergedPdf.getDirectory().getDirectory())
                .build();

        expectedModel.addPdf(pdfToTest);
        expectedModel.commitPdfBook();
        assertMergeCommandSuccess(pdfToTest, newMergedPdf, model,
                commandHistory, expectedMessage, expectedModel, result);
        deletePdf(newMergedPdf);
    }

    @Test
    public void execute_encryptedFile_failure() throws CommandException {
        decryptPdf(SAMPLE_PDF_1);
        encryptPdf(SAMPLE_PDF_1);
        String expectedMessage = MESSAGE_MERGE_PDF_ENCRYPT;

        MergeCommand mergeCommand = new MergeCommand(INDEX_FIRST_PDF, INDEX_SECOND_PDF);
        assertCommandFailure(mergeCommand, model, commandHistory, expectedMessage);

        decryptPdf(SAMPLE_PDF_1);
    }

    @Test
    public void execute_invalidPdfIndexUnfilteredList_failure() {
        Index outOfBoundIndex = Index.fromOneBased(model.getFilteredPdfList().size() + 1);
        Index validIndex = Index.fromZeroBased(1);
        MergeCommand mergeCommand = new MergeCommand(outOfBoundIndex, validIndex);

        assertCommandFailure(mergeCommand, model, commandHistory, Messages.MESSAGE_INVALID_PDF_DISPLAYED_INDEX);
    }

    @Test
    public void equals() {
        final MergeCommand standardCommand = new MergeCommand(INDEX_FIRST_PDF, INDEX_SECOND_PDF, INDEX_THIRD_PDF);

        // same value -> returns true
        MergeCommand standardCommandCopy = new MergeCommand(INDEX_FIRST_PDF, INDEX_SECOND_PDF, INDEX_THIRD_PDF);
        assertTrue(standardCommand.equals(standardCommandCopy));

        // same object -> returns true
        assertTrue(standardCommand.equals(standardCommand));

        // null -> returns false
        assertFalse(standardCommand.equals(null));

        // different types -> returns false
        assertFalse(standardCommand.equals(new ClearCommand()));

        // different index -> returns false
        assertFalse(standardCommand.equals(new MergeCommand(INDEX_FIRST_PDF, INDEX_SECOND_PDF)));
    }


    /**
     * Encrypts {@code pdfToEncrypt}.
     */
    @SuppressWarnings("Duplicates")
    private static void encryptPdf(Pdf pdfToEncrypt) throws CommandException {
        try {
            PDDocument file = PDDocument.load(Paths.get(pdfToEncrypt.getDirectory().getDirectory(),
                    pdfToEncrypt.getName().getFullName()).toFile());
            AccessPermission ap = new AccessPermission();
            StandardProtectionPolicy spp = new StandardProtectionPolicy(PASSWORD_1_VALID, PASSWORD_1_VALID, ap);

            spp.setEncryptionKeyLength(ENCRYPTION_KEY_LENGTH);
            spp.setPermissions(ap);
            file.protect(spp);
            file.save(Paths.get(pdfToEncrypt.getDirectory().getDirectory(),
                    pdfToEncrypt.getName().getFullName()).toFile());
            file.close();
        } catch (IOException ioe) {
            System.out.println();
        }
    }

    /**
     * Decrypts {code pdfToDecrypt}.
     */
    private static void decryptPdf(Pdf pdfToDecrypt) throws CommandException {
        try {
            PDDocument file = PDDocument.load(new File(pdfToDecrypt.getDirectory().getDirectory(),
                    pdfToDecrypt.getName().getFullName()), PASSWORD_1_VALID);
            file.setAllSecurityToBeRemoved(true);
            file.save(Paths.get(pdfToDecrypt.getDirectory().getDirectory(),
                    pdfToDecrypt.getName().getFullName()).toFile());
            file.close();
        } catch (IOException ioe) {
            throw new CommandException(String.format(MESSAGE_DECRYPT_PDF_FAILURE, pdfToDecrypt.getName()));
        }
    }

    /**
     * Deletes {code pdfToDelete} that was generated from the test
     */
    private static void deletePdf(Pdf pdfToDelete) {
        Paths.get(pdfToDelete.getDirectory().getDirectory(),
                pdfToDelete.getName().getFullName()).toFile().delete();
    }

}
