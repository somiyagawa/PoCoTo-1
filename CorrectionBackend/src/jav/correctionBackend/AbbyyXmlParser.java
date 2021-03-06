package jav.correctionBackend;

import jav.logging.log4j.Log;
import java.util.regex.Pattern;
import org.xml.sax.Attributes;

/**
 * Copyright (c) 2012, IMPACT working group at the Centrum für Informations- und
 * Sprachverarbeitung, University of Munich. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * This file is part of the ocr-postcorrection tool developed by the IMPACT
 * working group at the Centrum für Informations- und Sprachverarbeitung,
 * University of Munich. For further information and contacts visit
 * http://ocr.cis.uni-muenchen.de/
 *
 * @author thorsten (thorsten.vobl@googlemail.com)
 */
public class AbbyyXmlParser extends BaseSaxOcrDocumentParser {

    private int orig_id = 1;
    private int tokenIndex_ = 0;
    private int top_ = 0;
    private int bottom_ = 0;
    private int left_ = 0;
    private int right_ = 0;
    private int right_temp = 0;
    private int left_temp = 0;
    private String temp_ = "";
    private String lastchar_;
    private String thischar_;
    private int pages = 0;
    private boolean globalIsSuspicious = false;
    private boolean inVariant_ = false;
    private boolean isSuspicious_ = false;
    private boolean isDict_ = false;
    private Token temptoken_ = null;
    private int position_;
    private final static Pattern myAlnum
            = Pattern.compile("[\\pL\\pM\\p{Nd}\\p{Nl}\\p{Pc}[\\p{InEnclosedAlphanumerics}&&\\p{So}]]+");

    ;

    public AbbyyXmlParser(Document d) {
        super(d);
    }

    @Override
    public void startDocument() {
        tokensPerPage_ = 0;
    }

    @Override
    public void endDocument() {
        Log.info(
                this,
                "Loaded Document with %d page(s) and %d tokens",
                pages,
                tokensPerPage_
        );
    }

    @Override
    public void startElement(String uri, String nname, String qName, Attributes atts) {
        if (qName.equals("document")) {
        } else if (qName.equals("page")) {
            tokensPerPage_ = 0;
        } else if (qName.equals("block")) {
        } else if (qName.equals("region")) {
        } else if (qName.equals("rect")) {
        } else if (qName.equals("text")) {
        } else if (qName.equals("par")) {
        } else if (qName.equals("line")) {
            top_ = Integer.parseInt(atts.getValue("t"));
            if (top_ == -1) {
                top_ = 1;
            }
            bottom_ = Integer.parseInt(atts.getValue("b"));
            if (bottom_ == -1) {
                bottom_ = 1;
            }
        } else if (qName.equals("variantText")) {
            inVariant_ = true;
        } else if (qName.equals("formatting")) {
        } else if (qName.equals("charParams")) {
            this.isSuspicious_ = (atts.getValue("suspicious") != null);
            this.isDict_ = Boolean.parseBoolean(atts.getValue("wordFromDictionary"));

            left_temp = Integer.parseInt(atts.getValue("l"));
            right_temp = Integer.parseInt(atts.getValue("r"));
            position_++;
        }
    }

    @Override
    public void endElement(String uri, String nname, String qName) {
        if (qName.equals("document")) {
        } else if (qName.equals("page")) {
            orig_id = 1;
            if (tokensPerPage_ == 0) {
                handleEmptyPage(tokenIndex_++, pages);
            }
            tokensPerPage_ = 0;
            pages++;
        } else if (qName.equals("block")) {
        } else if (qName.equals("region")) {
        } else if (qName.equals("rect")) {
        } else if (qName.equals("text")) {
        } else if (qName.equals("par")) {

            temptoken_ = new Token("\n");
            temptoken_.setSpecialSeq(SpecialSequenceType.NEWLINE);
            temptoken_.setIndexInDocument(tokenIndex_);
            temptoken_.setIsSuspicious(false);
            temptoken_.setIsCorrected(false);
            temptoken_.setIsNormal(false);
            temptoken_.setNumberOfCandidates(0);
            temptoken_.setPageIndex(pages);
            temptoken_.setTokenImageInfoBox(null);

            getDocument().addToken(temptoken_);
            tokenIndex_++;
            temptoken_ = null;
            position_ = 0;
            left_ = 0;
            temp_ = "";

            // at end of line, pushback actual token and add newline token
        } else if (qName.equals("line")) {

            if (!temp_.equals("")) {
                temptoken_ = new Token(temp_);
                if (temp_.matches("^[\\p{Space}]+$")) {
                    temptoken_.setSpecialSeq(SpecialSequenceType.SPACE);
                } //                else if (temp_.matches("^[\\p{Punct}]+$")) {
                //                    temptoken_.setSpecialSeq(SpecialSequenceType.PUNCTUATION);
                //                }
                else if (temp_.matches("^[\n\r\f]+$")) {
                    temptoken_.setSpecialSeq(SpecialSequenceType.NEWLINE);
                } else {
                    temptoken_.setSpecialSeq(SpecialSequenceType.NORMAL);
                }
                temptoken_.setIndexInDocument(tokenIndex_);
                temptoken_.setIsSuspicious(this.globalIsSuspicious);
                temptoken_.setIsCorrected(false);
                temptoken_.setPageIndex(pages);
                temptoken_.setIsNormal(myAlnum.matcher(temp_).matches());
                temptoken_.setNumberOfCandidates(0);

                if (left_ > 0 && !temptoken_.getSpecialSeq().equals(SpecialSequenceType.SPACE)) {
                    TokenImageInfoBox tiib = new TokenImageInfoBox();
                    tiib.setCoordinateBottom(bottom_);
                    tiib.setCoordinateLeft(left_);
                    tiib.setCoordinateRight(right_);
                    tiib.setCoordinateTop(top_);
                    tiib.setImageFileName(getImageFile());
                    temptoken_.setTokenImageInfoBox(tiib);
                } else {
                    temptoken_.setTokenImageInfoBox(null);
                }

                temptoken_.setOrigID(orig_id);
                getDocument().addToken(temptoken_);
                this.globalIsSuspicious = false;
                orig_id++;
                tokenIndex_++;
            }

            temptoken_ = new Token("\n");
            temptoken_.setSpecialSeq(SpecialSequenceType.NEWLINE);
            temptoken_.setIndexInDocument(tokenIndex_);
            temptoken_.setIsSuspicious(false);
            temptoken_.setIsCorrected(false);
            temptoken_.setIsNormal(false);
            temptoken_.setNumberOfCandidates(0);
            temptoken_.setPageIndex(pages);
            temptoken_.setTokenImageInfoBox(null);

            getDocument().addToken(temptoken_);
            tokensPerPage_++;
            tokenIndex_++;
            temptoken_ = null;
            position_ = 0;
            left_ = 0;
            temp_ = "";

        } else if (qName.equals("variantText")) {
            inVariant_ = false;
        } else if (qName.equals("formatting")) {
        } else if (qName.equals("charParams")) {

            if (!inVariant_) {

                // tokenstring empty (happens at begin of document and after closing </line> and </par> tags)
                if (temp_.equals("")) {
                    temp_ = thischar_;
                } else {

                    // previous char alnum and actual char alnum -> attach thischar_ to tempstring
                    if (myAlnum.matcher(lastchar_).matches() && myAlnum.matcher(thischar_).matches()) {
                        temp_ += thischar_;

                        // previous char non-alnum and actual char alnum -> pushback token, attach thischar_ to tempstring
                    } else if (!myAlnum.matcher(lastchar_).matches() && myAlnum.matcher(thischar_).matches()) {

                        temptoken_ = new Token(temp_);
                        if (temp_.matches("^[\\p{Space}]+$")) {
                            temptoken_.setSpecialSeq(SpecialSequenceType.SPACE);
                        } else if (temp_.matches("^[\\p{Punct}]+$")) {
                            temptoken_.setSpecialSeq(SpecialSequenceType.PUNCTUATION);
                        } else if (temp_.matches("^[\n\r\f]+$")) {
                            temptoken_.setSpecialSeq(SpecialSequenceType.NEWLINE);
                        } else {
                            temptoken_.setSpecialSeq(SpecialSequenceType.NORMAL);
                        }
                        temptoken_.setIndexInDocument(tokenIndex_);
                        temptoken_.setIsSuspicious(this.globalIsSuspicious);
                        temptoken_.setIsCorrected(false);
                        temptoken_.setPageIndex(pages);
                        temptoken_.setIsNormal(myAlnum.matcher(temp_).matches());
                        temptoken_.setNumberOfCandidates(0);

                        // if document has coordinates
                        if (left_ > 0 && !temptoken_.getSpecialSeq().equals(SpecialSequenceType.SPACE)) {
                            TokenImageInfoBox tiib = new TokenImageInfoBox();
                            tiib.setCoordinateBottom(bottom_);
                            tiib.setCoordinateLeft(left_);
                            tiib.setCoordinateRight(right_);
                            tiib.setCoordinateTop(top_);
                            tiib.setImageFileName(getImageFile());
                            temptoken_.setTokenImageInfoBox(tiib);
                        } else {
                            temptoken_.setTokenImageInfoBox(null);
                        }

                        temptoken_.setOrigID(orig_id);
                        getDocument().addToken(temptoken_);
                        tokensPerPage_++;
                        this.globalIsSuspicious = false;
                        orig_id++;
                        tokenIndex_++;
                        temptoken_ = null;
                        position_ = 0;
                        left_ = 0;
                        temp_ = thischar_;

                        // previous char alnum and actual char non-alnum -> pushback token, attach thischar_ to tempstring
                    } else if (myAlnum.matcher(lastchar_).matches() & !myAlnum.matcher(thischar_).matches()) {

                        temptoken_ = new Token(temp_);
                        if (temp_.matches("^[\\p{Space}]+$")) {
                            temptoken_.setSpecialSeq(SpecialSequenceType.SPACE);
                        } //                        else if (temp_.matches("^[\\p{Punct}]+$")) {
                        //                            temptoken_.setSpecialSeq(SpecialSequenceType.PUNCTUATION);
                        //                        }
                        else if (temp_.matches("^[\n\r\f]+$")) {
                            temptoken_.setSpecialSeq(SpecialSequenceType.NEWLINE);
                        } else {
                            temptoken_.setSpecialSeq(SpecialSequenceType.NORMAL);
                        }
                        temptoken_.setIndexInDocument(tokenIndex_);
                        temptoken_.setIsSuspicious(this.globalIsSuspicious);
                        temptoken_.setIsCorrected(false);
                        temptoken_.setPageIndex(pages);
                        temptoken_.setIsNormal(myAlnum.matcher(temp_).matches());
                        temptoken_.setNumberOfCandidates(0);

                        // if document has coordinates
                        if (left_ > 0 && !temptoken_.getSpecialSeq().equals(SpecialSequenceType.SPACE)) {
                            TokenImageInfoBox tiib = new TokenImageInfoBox();
                            tiib.setCoordinateBottom(bottom_);
                            tiib.setCoordinateLeft(left_);
                            tiib.setCoordinateRight(right_);
                            tiib.setCoordinateTop(top_);
                            tiib.setImageFileName(getImageFile());
                            temptoken_.setTokenImageInfoBox(tiib);
                        } else {
                            temptoken_.setTokenImageInfoBox(null);
                        }

                        temptoken_.setOrigID(orig_id);
                        getDocument().addToken(temptoken_);
                        tokensPerPage_++;
                        this.globalIsSuspicious = false;
                        tokenIndex_++;
                        orig_id++;
                        temptoken_ = null;
                        position_ = 0;
                        temp_ = thischar_;
                        left_ = 0;

                        // previous char non-alnum and actual char non-alnum -> attach tempchar_ to token
                        // IS THIS REALLY '&' ? not '&&'?
                    } else if (!myAlnum.matcher(lastchar_).matches() & !myAlnum.matcher(thischar_).matches()) {
                        temp_ += thischar_;
                    }
                }
                lastchar_ = thischar_;
            }

            if (this.isSuspicious_ && !this.isDict_) {
                this.globalIsSuspicious = true;
            }

            // if left unset set it
            if (left_ == 0) {
                left_ = left_temp;
            }
            // set new right coordinate
            right_ = right_temp;
        }
    }

    /*
     * Assumption: abbyy xml output is charwise
     */
    @Override
    public void characters(char ch[], int start, int length) {
        thischar_ = new String(ch, start, length);
    }
}
