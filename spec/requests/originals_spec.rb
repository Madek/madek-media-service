describe "Originals" do
  let(:file) { File.read("spec/support/files/small.txt") }
  let(:file_size) { 132 }
  let(:part_1) { File.read("spec/support/files/small_01") }
  let(:part_2) { File.read("spec/support/files/small_02") }
  let(:md5) { Digest::MD5.hexdigest(file) }
  let(:part_1_md5) { Digest::MD5.hexdigest(part_1) }
  let(:part_2_md5) { Digest::MD5.hexdigest(part_2) }
  let(:store) { create(:media_store, :database, :with_users, users: [user]) }
  let(:start_request) { faraday_client_with_token.post("settings/uploads/#{upload_id}/start") }
  let(:complete_request) do
    faraday_client_with_token.post("settings/uploads/#{upload_id}/complete")
  end
  let(:upload_request) do
    faraday_client_with_token(json_response: true)
      .post(
        "settings/uploads/",
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
  # let(:request) { faraday_client_with_token.get("originals/#{original_id}") }
  def upload_get_request
    faraday_client_with_token(json_response: true).get("settings/uploads/#{upload_id}")
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
      puts "loop"
      sleep 0.1
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

  describe "GET: originals/:original_id/content?download=false" do
    let(:request) { faraday_client_with_token.get("originals/#{original_id}/content", { download: false}) }
    let(:response) { request }
    let(:user) { create(:user, :with_system_admin_role) }

    it "responds with success" do
      expect(response.status).to eq(200)
    end

    it "responds with the same file content as original" do
      expect(response.body).to eq(file)
    end
  end
end
