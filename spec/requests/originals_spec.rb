describe "Originals" do
  let(:file) { File.read("spec/support/files/small.txt") }
  let(:file_size) { 132 }
  let(:part_1) { File.read("spec/support/files/small_01") }
  let(:part_2) { File.read("spec/support/files/small_02") }
  let(:md5) { Digest::MD5.hexdigest(file) }
  let(:part_1_md5) { Digest::MD5.hexdigest(part_1) }
  let(:part_2_md5) { Digest::MD5.hexdigest(part_2) }
  let(:store) { create(:media_store, :database, :with_users, users: [user]) }
  let(:start_request) { faraday_client_with_token.post("uploads/#{upload_id}/start") }
  let(:complete_request) do
    faraday_client_with_token.post("uploads/#{upload_id}/complete")
  end
  let(:upload_request) do
    faraday_client_with_token(json_response: true)
      .post(
        "uploads/",
        content_type: "text/plain",
        filename: "small.txt",
        md5: md5,
        media_store_id: store.id,
        size: file_size
      )
  end
  let(:upload_id) { upload_request.body.fetch("id") }
  let(:part_1_request) do
    faraday_client_for_upload.put("parts/0", part_1) do |req|
      req.params = {
        start: 0,
        size: chunk_size,
        md5: part_1_md5
      }
    end
  end
  let(:part_2_request) do
    faraday_client_for_upload.put("parts/1", part_2) do |req|
      req.params = {
        start: chunk_size,
        size: 32,
        md5: part_2_md5
      }
    end
  end
  def upload_get_request
    faraday_client_with_token(json_response: true).get("uploads/#{upload_id}")
  end
  let(:api_token) { create(:api_token, user: user, scope_write: true) }
  let(:user_token) { api_token.token_hash }
  let(:chunk_size) { 100 }

  before do
    upload_request
    start_request
    part_1_request
    part_2_request
    complete_request
  end

  let(:original_id) do
    loop do
      media_file_id = upload_get_request.body["media_file_id"]
      break media_file_id if media_file_id.present?
      sleep 0.5
    end
  end

  describe "GET: originals/:original_id" do
    let(:request) { faraday_client_with_token(json_response: true).get("originals/#{original_id}") }
    let(:response) { request }
    let(:user) { create(:user, :with_system_admin_role) }

    it "responds with success" do
      expect(response.status).to eq(200)
    end

    it "responds with media file details" do
      expect(without_timestamps(response.body)).to match({
        id: a_kind_of(String),
        height: nil,
        size: file_size,
        width: nil,
        access_hash: nil,
        meta_data: nil,
        content_type: "text/plain",
        filename: "small.txt",
        guid: nil,
        extension: nil,
        media_type: nil,
        media_entry_id: nil,
        uploader_id: user.id,
        conversion_profiles: [],
        media_store_id: store.id,
        sha256: Digest::SHA256.hexdigest(file)
      }.stringify_keys)
    end
  end

  describe "GET: originals/:original_id/content?download=true" do
    let(:request) { faraday_client_with_token.get("originals/#{original_id}/content", { download: true }) }
    let(:response) { request }
    let(:user) { create(:user, :with_system_admin_role) }

    it "responds with success" do
      expect(response.status).to eq(200)
    end

    it "responds with a correct Content-Type header" do
      expect(response.headers["Content-Type"]).to eq("text/plain")
    end

    it "responds with a correct Content-Length header" do
      expect(response.headers["Content-Length"]).to eq(file_size.to_s)
    end

    it "responds with a correct Content-Disposition header" do
      expect(response.headers["Content-Disposition"]).to eq(%(attachment; filename="small.txt"))
    end

    it "responds with the same file content as original" do
      expect(response.body).to eq(file)
    end
  end

  describe "GET: originals/:original_id/content?download=false" do
    let(:request) { faraday_client_with_token.get("originals/#{original_id}/content", { download: false}) }
    let(:response) { request }
    let(:user) { create(:user, :with_system_admin_role) }

    it "responds with success" do
      expect(response.status).to eq(200)
    end

    it "responds with a correct Content-Type header" do
      expect(response.headers["Content-Type"]).to eq("text/plain")
    end

    it "responds with a correct Content-Length header" do
      expect(response.headers["Content-Length"]).to eq(file_size.to_s)
    end

    it "responds with a correct Content-Disposition header" do
      expect(response.headers["Content-Disposition"]).to be_nil
    end

    it "responds with the same file content as original" do
      expect(response.body).to eq(file)
    end
  end

  describe "Range header" do
    let(:request) do
      faraday_client_with_token.get("originals/#{original_id}/content") do |req|
        req["Range"] = "bytes=#{range}"
      end
    end
    let(:response) { request }
    let(:user) { create(:user, :with_system_admin_role) }

    context "for range: all bytes from part 1" do
      let(:range) { "0-99" }

      it "responds with '206 Partial Content' status code" do
        expect(response.status).to eq(206)
      end

      it "responds with a correct Content-Range header" do
        expect(response.headers["Content-Range"]).to eq("bytes #{range}/#{file_size}")
      end

      it "responds with a correct Content-Length header" do
        expect(response.headers["Content-Length"]).to eq("100")
      end

      it "responds with the correct data" do
        expect(response.body).to eq(part_1)
      end
    end

    context "for range: all bytes from part 2" do
      let(:range) { "100-131" }

      it "responds with '206 Partial Content' status code" do
        expect(response.status).to eq(206)
      end

      it "responds with a correct Content-Range header" do
        expect(response.headers["Content-Range"]).to eq("bytes #{range}/#{file_size}")
      end

      it "responds with a correct Content-Length header" do
        expect(response.headers["Content-Length"]).to eq("32")
      end

      it "responds with the correct data" do
        expect(response.body).to eq(part_2)
      end
    end

    context "for range: first 12 bytes" do
      let(:range) { "0-11" }

      it "responds with '206 Partial Content' status code" do
        expect(response.status).to eq(206)
      end

      it "responds with a correct Content-Range header" do
        expect(response.headers["Content-Range"]).to eq("bytes #{range}/#{file_size}")
      end

      it "responds with a correct Content-Length header" do
        expect(response.headers["Content-Length"]).to eq("12")
      end

      it "responds with the correct data" do
        expect(response.body).to eq("0123456789\n0")
      end
    end

    context "for range: 4 bytes from the middle of the part 1" do
      let(:range) { "54-57" }

      it "responds with '206 Partial Content' status code" do
        expect(response.status).to eq(206)
      end

      it "responds with a correct Content-Range header" do
        expect(response.headers["Content-Range"]).to eq("bytes #{range}/#{file_size}")
      end

      it "responds with a correct Content-Length header" do
        expect(response.headers["Content-Length"]).to eq("4")
      end

      it "responds with the correct data" do
        expect(response.body).to eq("\n012")
      end
    end

    context "for range: last 3 bytes from part 1 and 2 bytes from part 2" do
      let(:range) { "97-101" }

      it "responds with '206 Partial Content' status code" do
        expect(response.status).to eq(206)
      end

      it "responds with a correct Content-Range header" do
        expect(response.headers["Content-Range"]).to eq("bytes #{range}/#{file_size}")
      end

      it "responds with a correct Content-Length header" do
        expect(response.headers["Content-Length"]).to eq("5")
      end

      it "responds with the correct data" do
        expect(response.body).to eq("9\n012")
      end
    end

    context "for range: last 2 bytes from part 1 and all bytes from part 2" do
      let(:range) { "98-131" }

      it "responds with '206 Partial Content' status code" do
        expect(response.status).to eq(206)
      end

      it "responds with a correct Content-Range header" do
        expect(response.headers["Content-Range"]).to eq("bytes #{range}/#{file_size}")
      end

      it "responds with a correct Content-Length header" do
        expect(response.headers["Content-Length"]).to eq("34")
      end

      it "responds with the correct data" do
        expect(response.body).to eq("\n0123456789\n0123456789\n0123456789\n")
      end
    end

    context "for range: 5 bytes from the middle of the part 2" do
      let(:range) { "109-113" }

      it "responds with '206 Partial Content' status code" do
        expect(response.status).to eq(206)
      end

      it "responds with a correct Content-Range header" do
        expect(response.headers["Content-Range"]).to eq("bytes #{range}/#{file_size}")
      end

      it "responds with a correct Content-Length header" do
        expect(response.headers["Content-Length"]).to eq("5")
      end

      it "responds with the correct data" do
        expect(response.body).to eq("\n0123")
      end
    end

    context "for range: last 3 bytes from the part 2" do
      let(:range) { "129-131" }

      it "responds with '206 Partial Content' status code" do
        expect(response.status).to eq(206)
      end

      it "responds with a correct Content-Range header" do
        expect(response.headers["Content-Range"]).to eq("bytes #{range}/#{file_size}")
      end

      it "responds with a correct Content-Length header" do
        expect(response.headers["Content-Length"]).to eq("3")
      end

      it "responds with the correct data" do
        expect(response.body).to eq("89\n")
      end
    end
  end
end
