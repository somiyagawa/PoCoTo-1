/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jav.correctionBackend.export;

import jav.correctionBackend.Document;
import jav.correctionBackend.MyIterator;
import jav.correctionBackend.Page;
import jav.correctionBackend.SpecialSequenceType;
import jav.correctionBackend.Token;
import java.util.ArrayList;

/**
 *
 * @author finkf
 */
public class DocumentLineReader implements LineReadeable {

    private final ArrayList<String> lines;

    public DocumentLineReader(Page page, Document doc) {
        lines = new ArrayList<>();
        readLines(page, doc);
    }

    @Override
    public int getNumberOfLines() {
        return lines.size();
    }

    @Override
    public String getLineAt(int i) {
        return lines.get(i);
    }

    private void readLines(Page page, Document doc) {
        MyIterator<Token> tokens = doc.tokenIterator(page);
        StringBuilder line = new StringBuilder();
        while (tokens.hasNext()) {
            Token token = tokens.next();
            if (token.getSpecialSeq() == SpecialSequenceType.NEWLINE) {
                lines.add(line.toString());
                lines.clear();
            } else {
                line.append(token.getWOCR());
            }
        }
    }

}
